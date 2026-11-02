# Integration coverage and execution notes

164 specified cases; not executed. Template fonts, colors, borders and case-row styles are preserved from original OOXML; metadata wrapping/date format variants are added for readability.

|Unit/sheet|Cases|Primary UC accounted for|
|---|---:|---|
|U01 Account Access|13|01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12|
|U02 Audit|5|13|
|U03 Jobs Settings|11|14, 15|
|U04 Subjects|6|16, 17, 18, 19|
|U05 Classes Enrollment|10|20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30|
|U06 AI Connections|14|31, 32, 33|
|U07 Assignment Authoring|12|34, 35, 36|
|U08 Assignment Work|14|37, 38, 39, 40, 41|
|U09 Assignment Grading|15|42, 43, 44, 45|
|U10 Quiz Authoring|8|46, 47, 48|
|U11 Quiz Practice|8|49, 50, 51, 52|
|U12 Group Assignment|14|53, 54, 55, 56, 57|
|U13 Materials|8|58, 59, 60, 61, 62|
|U14 Notifications|11|63, 64, 65|
|U15 Admin Dashboard|5|66|
|SecurityTest|10|02, 07, 13, 14, 15, 21, 23, 25, 28, 32, 39, 40, 44, 52, 56, 62, 64, 66|

## Source hierarchy

Current unit business-rules and current contracts/services govern expected behavior. The 2026-11-01 external-agent amendment supersedes older MCP progress rules. Historical report example is a writing reference, not product requirements. No UI-only placeholder cases were counted.

## Traceability

### U01 Account Access

|Case|UC|Source rule|
|---|---|---|
|IT-U01-001|8,9,10|BR-U01-10/11|
|IT-U01-002|1,2|BR-U01-12/13/24|
|IT-U01-003|1|BR-U01-23/26|
|IT-U01-004|1,4|BR-U01-21/22/28|
|IT-U01-005|2|BR-U01-40/44/48|
|IT-U01-006|2|BR-U01-40/41|
|IT-U01-007|3|BR-U01-45|
|IT-U01-008|4|BR-U01-13/34|
|IT-U01-009|5|BR-U01-33/34|
|IT-U01-010|6,7|BR-U01-50/52|
|IT-U01-011|11|BR-U01-63/64|
|IT-U01-012|12|BR-U01-71; BR-U04-12|
|IT-U01-013|9|BR-U01-81/82/83/85|

### U02 Audit

|Case|UC|Source rule|
|---|---|---|
|IT-U02-001|13|BR-U02-02/09|
|IT-U02-002|13|BR-U02-02/04|
|IT-U02-003|13|BR-U02-02|
|IT-U02-004|13|BR-U02-03|
|IT-U02-005|13|BR-U02-05/07|

### U03 Jobs Settings

|Case|UC|Source rule|
|---|---|---|
|IT-U03-001|14,15|BR-U03-82/83/87|
|IT-U03-002|15|BR-U03-82|
|IT-U03-003|14|BR-U03-89|
|IT-U03-004|15|BR-U03-03/06|
|IT-U03-005|15|BR-U03-06/41|
|IT-U03-006|15|BR-U03-30|
|IT-U03-007|15|BR-U03-50/51/59|
|IT-U03-008|15|BR-U03-50|
|IT-U03-009|15|BR-U03-88/90|
|IT-U03-010|14,15|BR-U03-93/95/96|
|IT-U03-011|14|BR-U03-98|

### U04 Subjects

|Case|UC|Source rule|
|---|---|---|
|IT-U04-001|16,17,18|BR-U04-03/50|
|IT-U04-002|19|BR-U04-03/10|
|IT-U04-003|17,19|BR-U04-03/53|
|IT-U04-004|19|BR-U04-04/11|
|IT-U04-005|19|BR-U04-04|
|IT-U04-006|19|BR-U04-04/11|

### U05 Classes Enrollment

|Case|UC|Source rule|
|---|---|---|
|IT-U05-001|24,25,26|BR-U05-10/11/49|
|IT-U05-002|27,22,23|BR-U05-12/30/46|
|IT-U05-003|28,20,21|BR-U05-20/21/40/41|
|IT-U05-004|28,21|BR-U05-25/30|
|IT-U05-005|28|BR-U05-21/22|
|IT-U05-006|28|BR-U05-21/25|
|IT-U05-007|28|BR-U05-23/24|
|IT-U05-008|29,20,21|BR-U05-14/26/40|
|IT-U05-009|29,20|BR-U05-15/16|
|IT-U05-010|30|BR-U05-48|

### U06 AI Connections

|Case|UC|Source rule|
|---|---|---|
|IT-U06-001|31|BR-U06-01/03/06|
|IT-U06-002|31|BR-U06-04|
|IT-U06-003|31,33|BR-U06-05/10|
|IT-U06-004|33|BR-U06-12/13|
|IT-U06-005|33|BR-U06-13/14|
|IT-U06-006|33|BR-U06-14/25|
|IT-U06-007|33|BR-U06-21|
|IT-U06-008|32|BR-U06-30/31/33|
|IT-U06-009|32|BR-U06-31|
|IT-U06-010|32,33|BR-U06-16/35/40|
|IT-U06-011|33|BR-U06-16/40/41|
|IT-U06-012|32|EA03; BR-U09 external-agent amendment|
|IT-U06-013|32|EA03; BR-U09 external-agent amendment|
|IT-U06-014|32|EA02/03|

### U07 Assignment Authoring

|Case|UC|Source rule|
|---|---|---|
|IT-U07-001|34,35,36|BR-U07-10/11/20|
|IT-U07-002|35,36|BR-U07-23/24/32/33|
|IT-U07-003|35|BR-U07-30/33/34|
|IT-U07-004|35|BR-U07-02/33|
|IT-U07-005|35|BR-U07-36; BR-U08-23|
|IT-U07-006|35|BR-U07-40/43/25|
|IT-U07-007|35|BR-U07-41|
|IT-U07-008|35|BR-U07-42/43|
|IT-U07-009|35|BR-U07-51/53|
|IT-U07-010|35|BR-U07-51/70|
|IT-U07-011|35|BR-U07-60/61/62|
|IT-U07-012|35|BR-U07-12/15|

### U08 Assignment Work

|Case|UC|Source rule|
|---|---|---|
|IT-U08-001|37,38|BR-U08-01/02/03|
|IT-U08-002|39|BR-U08-10/11/12|
|IT-U08-003|39|BR-U08-13|
|IT-U08-004|39|BR-U08-13/14|
|IT-U08-005|39|BR-U08-14; BR-U07-15|
|IT-U08-006|39,40|BR-U08-20/21/22|
|IT-U08-007|39|BR-U08-21; BR-U03-64|
|IT-U08-008|39|BR-U08-12/15/20|
|IT-U08-009|39,40|BR-U08-11/30|
|IT-U08-010|39|BR-U08-23|
|IT-U08-011|40,41|BR-U08-40/41/42/43|
|IT-U08-012|41|BR-U08-40/45|
|IT-U08-013|41|BR-U08-44|
|IT-U08-014|39|BR-U08-50/52/53/55|

### U09 Assignment Grading

|Case|UC|Source rule|
|---|---|---|
|IT-U09-001|42|BR-U09-02/10/11|
|IT-U09-002|44|BR-U09-20/22|
|IT-U09-003|44|BR-U09-20/21|
|IT-U09-004|43|BR-U09-30/31/32/33|
|IT-U09-005|43|BR-U09-32/35|
|IT-U09-006|43|BR-U09-34|
|IT-U09-007|44|BR-U09-40|
|IT-U09-008|44,45|BR-U09-04/41|
|IT-U09-009|44|BR-U09-42/43|
|IT-U09-010|44|BR-U09-71|
|IT-U09-011|45|BR-U09-03/60|
|IT-U09-012|45|BR-U09-61|
|IT-U09-013|45|BR-U09-62/63|
|IT-U09-014|45|BR-U09-63|
|IT-U09-015|42|BR-U09-13; BR-U08-54|

### U10 Quiz Authoring

|Case|UC|Source rule|
|---|---|---|
|IT-U10-001|46,47|BR-U10-01/04/06|
|IT-U10-002|47,48|BR-U10-04/05|
|IT-U10-003|47|BR-U10-07|
|IT-U10-004|48|BR-U10-13/20/21/22|
|IT-U10-005|48|BR-U10-13; BR-U11-11|
|IT-U10-006|48|BR-U10-23; BR-U11-14|
|IT-U10-007|48|BR-U10-30/31|
|IT-U10-008|48|BR-U10-40/41/42|

### U11 Quiz Practice

|Case|UC|Source rule|
|---|---|---|
|IT-U11-001|49,50,51|BR-U11-01/03/10/11|
|IT-U11-002|51|BR-U11-12|
|IT-U11-003|51,52|BR-U11-13/15; BR-U10-16|
|IT-U11-004|51,52|BR-U10-16; BR-U11-15|
|IT-U11-005|51|BR-U11-11/14|
|IT-U11-006|52|BR-U11-20; BR-U10-15|
|IT-U11-007|49,50,52|BR-U11-02/20|
|IT-U11-008|51|BR-U11-30/31|

### U12 Group Assignment

|Case|UC|Source rule|
|---|---|---|
|IT-U12-001|57,54|BR-U12-01/02/04/50|
|IT-U12-002|57|BR-U12-02/04|
|IT-U12-003|57|BR-U12-03|
|IT-U12-004|55,57|BR-U12-10/11/13|
|IT-U12-005|55,57|BR-U12-10/11/12|
|IT-U12-006|53|BR-U12-20/22|
|IT-U12-007|56|BR-U12-30/33|
|IT-U12-008|56|BR-U12-34/35/37|
|IT-U12-009|56|BR-U12-36|
|IT-U12-010|56,57|BR-U12-07|
|IT-U12-011|56|BR-U12-40/41/44; BR-U09-50|
|IT-U12-012|56|BR-U12-41; BR-U03-64|
|IT-U12-013|56|BR-U12-42/43|
|IT-U12-014|56|BR-U12-60; BR-U09-50/51|

### U13 Materials

|Case|UC|Source rule|
|---|---|---|
|IT-U13-001|58,59,62|BR-U13-01/10/20/30|
|IT-U13-002|60,61,62|BR-U13-01/02|
|IT-U13-003|59,61|BR-U13-12|
|IT-U13-004|59,61,62|BR-U13-13/23|
|IT-U13-005|59,61,62|BR-U13-10/21|
|IT-U13-006|58,59|BR-U13-30/31|
|IT-U13-007|58,59|BR-U13-M01/M03/M04|
|IT-U13-008|62|BR-U13-23/31/32|

### U14 Notifications

|Case|UC|Source rule|
|---|---|---|
|IT-U14-001|63,64,65|BR-U14-01/03/10|
|IT-U14-002|64,65|BR-U14-03/20/21/23|
|IT-U14-003|64|BR-U14-04|
|IT-U14-004|64|BR-U14-05|
|IT-U14-005|64|BR-U14-02|
|IT-U14-006|65|BR-U14-13|
|IT-U14-007|65|BR-U14-12|
|IT-U14-008|65|BR-U14-10/42|
|IT-U14-009|65|BR-U14-22/23|
|IT-U14-010|65|BR-U14-30/31/32|
|IT-U14-011|65|BR-U14-23/24|

### U15 Admin Dashboard

|Case|UC|Source rule|
|---|---|---|
|IT-U15-001|66|BR-U15-10/11|
|IT-U15-002|66|BR-U15-12|
|IT-U15-003|66|BR-U15-20/21|
|IT-U15-004|66|BR-U15-01/02/30|
|IT-U15-005|66|U15 Activity projection 2026-10-30|

### SecurityTest

|Case|UC|Source rule|
|---|---|---|
|IT-SEC-001|7|BR-U01-51/52/54|
|IT-SEC-002|13|BR-U02-07/50|
|IT-SEC-003|14,15|BR-U03-80; BR-U06-51|
|IT-SEC-004|21,23,25|BR-U05-42/51/55|
|IT-SEC-005|39,40,52,56|BR-U08-32; BR-U11-22; BR-U12-31|
|IT-SEC-006|62|BR-U03-20/21/22|
|IT-SEC-007|32|BR-U06-30/34|
|IT-SEC-008|15,28,44,64|Common authorization/CSRF rules in current business designs|
|IT-SEC-009|2,32|BR-U01-95; BR-U06-53; EA03|
|IT-SEC-010|66|BR-U15-01|

## Rulings and limits

- Use artifact-tool for all workbook cell/formula authoring. Package those cells into original worksheet/style shells using lxml to preserve exact template borders/colors/fonts/theme and widths. No openpyxl authoring. The Windows node_modules junction was denied; load the public package entry through createRequire using the bundled dependency root.
- Source template has incorrect per-round formulas, metadata #REF! and obsolete ACTION names; correct affected references, extend exact count ranges and remove obsolete invalid names in the new report.
- Template placeholder row 12 is green italic example text. Real case rows inherit the regular black body row 19, including its original borders and Note style. Header and palette are unchanged.
- Template has no conditional-format rules. Preserve this; retain and correctly bound status validations instead of adding a new visual style. Remove copied feature example comments to avoid duplicating one shared comment part across 15 sheets; preserve actual template headers, legends and print setup.
- Summary uses Round 1; SecurityTest has its original single status field and maps Not Run/Blocked to Pending and Skip to N/A. Not Run/Blocked are not Passed.
- Common stubs-off requirement is in Test Environment only. Cases use fresh fixture copies and case-specific setup; generated IDs must be recorded at execution.
- Application cases, real-provider/model-quality acceptance, production recovery targets and native Microsoft Excel rendering are not claimed executed.
