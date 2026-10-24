# RMS backend (Spring Boot 3, Java 17, MySQL 8)

Standalone reinsurance system - treaty administration, maker-checker and master-data APIs on the RMS MySQL schema (65 tables). All entities in `com.rms.domain` are generated from that schema.
The schema is not created by the application (no Flyway): load it into the `rms` database before starting.

## Run
```
mysql -e "CREATE DATABASE rms CHARACTER SET utf8mb4; CREATE USER 'rms'@'%' IDENTIFIED BY 'rms'; GRANT ALL ON rms.* TO 'rms'@'%';"
mvn spring-boot:run
```
Swagger UI: http://localhost:8080/swagger-ui.html. Dev users (password `Passw0rd!`): `maker.ravi`, `maker.divya`, `checker.meena`,
`head.sundar` (final approver), `viewer.anil`. After the first successful start set `spring.jpa.hibernate.ddl-auto=validate` to verify the mapping.

## Treaty API (`/api/v1/treaties`)
| Call | Purpose |
|---|---|
| `POST /` | create a DRAFT (version 1) with all children (layers, panel, commission, mapping) |
| `PUT /{id}` | **edit / update**: DRAFT -> updated in place; ACTIVE / RUN_OFF -> creates the next DRAFT version (needs `effectiveFrom`, optional `amendModeId`); PENDING_APPROVAL -> 409 |
| `POST /{id}/submit` | validate (errors block) and open a maker-checker request; `POST /{id}/withdraw` takes it back |
| `POST /{id}/renew` | copy into a new treaty code / period |
| `POST /validate`, `GET /{id}/validate` | list of ERROR / WARNING issues |
| `POST /{id}/simulate` | dry-run a sample risk or loss through the configuration |
| `GET /{id}/diff?with={otherId}` | field-level compare of two versions |
| `GET /`, `GET /{id}`, `GET /code/{code}/versions`, `DELETE /{id}` (drafts) | read / list / soft-delete |

Maker-checker: `GET /api/v1/maker-checker/requests?scope=pending|mine|all`, `POST .../{id}/approve|reject|return`.
Two levels by default (`rms.makerchecker.levels.treaty`). The maker can never decide his own request; one user cannot approve two levels.

## Treaty mapping levels (LOB / product / section / cover)
A treaty mapping row can sit at **LOB, PRODUCT, SECTION or COVER** level (`treaty_cover_map.map_level`). `CoverHierarchyService` cross-checks every row:
* the deepest id supplied decides the level; missing ancestors are derived so the id/code/description snapshots are complete
* contradictions are rejected (cover not in section, section not in product, product not in LOB) - at save time (400 `COVER_MAP_INVALID`) and by `/validate`
* the mapping LOB must equal the treaty LOB
* a row expands to the set of covers it applies to; overlap checks (same sequence, overlapping dates) compare those sets, inside the treaty and against other ACTIVE / PENDING treaties

## Execution chain per cover: treaty_cover_map + treaty_cover_allocation_rule
| Table | Responsibility |
|---|---|
| `treaty_cover_map` | WHICH treaty applies (LOB / product / section / cover) and in WHICH ORDER (`sequence_no`), with effective dates |
| `treaty_cover_allocation_rule` | HOW the treaty takes part at that sequence: `allocation_method` (QS, SURPLUS, XOL, FAC, STOP_LOSS), `allocation_basis` (GROSS, NET_AFTER_QS, NET_AFTER_SURPLUS, NET_AFTER_QS_SURPLUS, NET_AFTER_XOL, NET_RETAINED, RESIDUAL, LOSS_AMOUNT), optional pct / min / max caps, `residual_action` (CONTINUE, RETAIN, FACULTATIVE, ERROR, UNALLOCATED), `stop_on_limit_flag`, condition, dates |
| `treaty_param_qs`, `treaty_surplus_retention/_layer`, `treaty_xol_layer`, `treaty_stoploss` | the numbers of each mapping row (QS %, retention, lines, layers), keyed by `treaty_cover_map_id` |
| `treaty_panel`, fac tables | reinsurer participation / actual facultative placement |

Method, basis and residual action are lookup values (id + code + description snapshot); the engine branches on the stored codes. Methods a row's treaty type may use: QS->QS, SURPLUS->SURPLUS, XOL->XOL, STOP->STOP_LOSS, FAC-OBL->FAC/QS, FAC-RES->FAC.

`AllocationEngine` (pure, deterministic) runs the steps strictly by (sequence, rule no): QS = pct of the basis (up to the per-risk limit); SURPLUS = basis above retention up to total line capacity, the rest is OVERFLOW;
XOL and STOP_LOSS protect the net retained account and do not consume exposure; FAC places the pool of overflow (basis RESIDUAL). The rule's residual action decides what happens to overflow;
with `stop_on_limit_flag = false` the overflow always continues. `AllocationPlanService.chainProblems` enforces ordering (a NET_AFTER_x basis needs an earlier step of method x, RESIDUAL is for FAC only and needs something earlier,
GROSS after an earlier allocation warns, surplus with nothing placing the overflow warns); the treaty validator runs it per cover against the other ACTIVE / PENDING treaties.

| Call | Purpose |
|---|---|
| `GET /api/v1/allocation/plan?coverId=&date=` | the ordered chain in force for a cover |
| `POST /api/v1/allocation/simulate` | run a sample risk (sum insured, premium, risk grade) through the chain |

Worked example from the sample data (Fire): SI 70m -> surplus (seq 1, gross): retained 5m, ceded 50m, overflow 15m -> cat XOL (seq 2, net after surplus) protects the 5m -> FAC residual (seq 3) places the 15m.
The engine is not yet wired into the ERP inbound processing: cession rows are still produced by the sample data / future placement service.

## Treaty type and terms per mapping row (Flyway V3)
The treaty header has **no type any more**. A treaty is the contract / programme (code, name, LOB, period, bases, broker, sliding scale / PC / DAF, panel);
each `treaty_cover_map` row says WHERE (LOB / product / section / cover), WHICH treaty type, in WHICH order (`sequence_no`) and on WHICH terms.
One treaty can therefore carry several types against the same product, e.g. sample `FIRE-PROG-2027` (db/sample/V4):

| Row | Applies to | Type | Seq | Terms |
|---|---|---|---|---|
| 10 | product Standard Fire | Quota share | 1 | 20% / flat 27.5% |
| 11 | section Property Damage | Surplus | 2 | retention 4m, 4 + 6 lines, on net after QS |
| 12 | cover Earthquake | Surplus | 2 | retention 2m, 3 lines (overrides row 11 for earthquake) |
| 13 | product Standard Fire | Facultative residual | 3 | none |

| Table | Change |
|---|---|
| `treaty_cover_map` | + `treaty_type_id/_code/_desc` (snapshot), + `note` |
| `treaty_param_qs`, `treaty_surplus_retention`, `treaty_surplus_layer`, `treaty_xol_layer`, `treaty_stoploss` | + `treaty_cover_map_id`: terms belong to one row (`treaty_id` still filled); layer numbers unique per row |
| `treaty_panel` | + `panel_group` (type code of the rows it serves) + `layer_no`: one panel per type / layer used (QS, SUR:1, SUR:2, XOL-CAT:1, ...) |
| `treaty.treaty_type_*` | now NULL-able, not used |
| `cession` | + `treaty_cover_map_id` (row whose type and terms produced it) |

Existing data is migrated in V3: every row takes its treaty's type; the treaty's terms go to its first row and are copied (XOL reinstatements included) to its other rows.
Terms of a treaty with no mapping row keep `treaty_cover_map_id` NULL until it is mapped.

Rules (`TreatyValidator`, `AllocationPlanService`):
* per row, only the terms of the row's type are stored and validated (QS / fac obligatory: `qsParams`; surplus: `retentions` + `surplusLayers`; XOL: `xolLayers`; stop loss: `stopLoss`; fac residual: none); allowed allocation methods follow the row type
* per cover and type, the most specific row of a treaty applies (cover > section > product > LOB); the same type twice at the same level on a cover is an error; two different types of one treaty at the same sequence on a cover is an error
* panel groups are derived from the rows; each needs 100% (fac residual optional); panel rows for a type no row uses are rejected
* sliding scale (for QS rows with sliding commission) and PC / DAF stay treaty-level
* the engine reads each step's numbers from its row and reports which row supplied them (`StepResult.termsSource`), plus commission per step (surplus layer by layer)
* API: `CoverMapDto` carries `treatyTypeId` + the terms; `TreatyRequest` has no type / terms; `TreatyView.treatyTypes` lists the types used; `PanelDto.layerKind` = type code;
  `SimulationRequest.mappingRow` picks the row to simulate; search `typeId` finds treaties having a row of that type

Verified: V1 -> V2 -> V3 -> V4 run on MariaDB with the expected data; 97 scenario checks pass (`mvn test`; compiled here against API stubs because Maven Central was unreachable).
Hibernate mapping and the security set-up still need one start against MySQL 8.

## Policy-wise and claim-wise distribution (`/api/v1`)
| Call | Purpose |
|---|---|
| `GET /policies?q=` , `GET /policies/distribution?policyNo=` | per transaction -> cover -> treaty -> layer -> reinsurer shares (SI, premium, commission), facultative placements, totals per reinsurer |
| `GET /claims?q=` , `GET /claims/distribution?claimNo=` | per claim transaction -> recovery -> reinsurer shares (paid / outstanding), cat events, totals per reinsurer |
| `POST /distribution/adjustments` | propose a new reinsurer split for ONE policy cession layer (`scope=POLICY`, `targetId`=layer id) or ONE claim recovery (`scope=CLAIM`) |

Policy and claim numbers contain slashes, so they are query parameters. A proposal needs the `DISTRIBUTION_MAKE` authority, a reason and shares totalling 100%.
It goes through maker-checker (`rms.makerchecker.levels.distribution`, default 1). Nothing changes until approval; then `DistributionApprovalHandler`
re-splits the stored layer / recovery totals (rounding remainder to the largest share), replaces the share rows and writes `distribution_adjustment`.
Reinsurer accounts already issued for the period are not rewritten - restate them separately.

## Notes
* Rule 3A: no enums for masters; transaction rows store master id + code + description (`MasterSnapshotService`).
* Lookup value codes used by workflow code are plain strings in `LookupCodes` (resolved against `ref_lookup_value`).
* Verification done without Maven / Spring jars: all sources were compiled with javac against thin stubs of the Spring / JPA / Jackson APIs (Lombok replaced by explicit accessors) - no errors.
  The real `CoverHierarchyService`, `TreatyValidator`, `TreatyMapper` and `TreatyService` were then run against in-memory fake repositories: hierarchy / overlap / allocation-rule / chain checks, create / update / amend / renew / delete checks and execution-chain engine checks passed (`src/test/java`, run with `mvn test`; no database needed). Not covered: JPQL / derived-query syntax, Hibernate mapping, Flyway DDL on MySQL and the security configuration - run the app once against MySQL.

## ERP policy API (`/api/v1/erp`) - tables `erp_policy_txn`, `erp_policy_cover`
Master references are ids; the server fills the code / description snapshots (txn type, currency, product, risk grade, section, cover).
Deletes are soft (`record_status = 'DELETED'`) and deleted rows are hidden everywhere, including the distribution screens.
View calls need `TREATY_VIEW`, write calls `TREATY_MAKE`. Send `rowVersion` on updates to get 409 `OPTIMISTIC_LOCK` on stale edits.

| Call | Purpose |
|---|---|
| `GET /policy-txns?q=&policyNo=&productId=&txnTypeId=&inboundMessageId=&txnDateFrom=&txnDateTo=&page=&size=&sort=` | paged list with cover count, total SI after, total premium |
| `GET /policy-txns/{id}` | one transaction with its covers and totals |
| `GET /policy-txns/by-policy?policyNo=` | all transactions of a policy, oldest first |
| `POST /policy-txns` | create a transaction, optionally with `covers: [...]` |
| `PUT /policy-txns/{id}` | update header; `covers` omitted = untouched, list = replace (id -> update, no id -> add, missing -> delete) |
| `DELETE /policy-txns/{id}` | soft-delete the transaction and its covers |
| `GET /policy-txns/{txnId}/covers`, `POST /policy-txns/{txnId}/covers` | list / add cover lines |
| `GET /policy-covers/{id}`, `PUT /policy-covers/{id}`, `DELETE /policy-covers/{id}` | one cover line |

Checks (422 `VALIDATION_FAILED` with `issues`): required fields and lengths, inbound message exists, expiry >= inception,
section belongs to the transaction's product, cover belongs to the section, a cover only once per transaction, SI after / deductible not negative.

Example:
```json
POST /api/v1/erp/policy-txns
{ "inboundMessageId": 1, "policyNo": "FIR/2026/0001", "txnTypeId": 41, "txnDate": "2026-04-01",
  "inceptionDate": "2026-04-01", "expiryDate": "2027-03-31", "currencyId": 1, "productId": 3,
  "insuredName": "ABC Textiles Ltd", "locationDesc": "Chennai", "catZoneCode": "TN-1", "riskGradeId": 12,
  "covers": [ { "sectionId": 5, "coverId": 9, "sumInsured": 70000000, "sumInsuredAfter": 70000000, "premium": 105000, "deductibleAmt": 100000 } ] }
```
