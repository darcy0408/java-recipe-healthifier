# Hardening plan

## Objective

Make Recipe Healthifier honest on realistic recipes, demonstrate Java 26 intentionally, and improve contest usability without compromising the working core.

## Completed

- Replace unconditional compliance with vocabulary-based evaluation.
- Apply multiple substitutions per ingredient using longest-source-first matching.
- Rewrite instructions using substitutions actually applied to ingredients.
- Ensure custom avoidances are always evaluated.
- Add adversarial tests based on realistic chicken parmesan ingredients.
- Expand the initial substitution set and choose replacements by compatibility with all selected goals.
- Prefer HTTP/3 through Java 26's final HTTP Client API.
- Remove unused preview compiler configuration and the unused preferences module.
- Add a responsive, phone-accessible local server with form validation and security controls.

## In progress

- Review the hardened rule vocabulary for additional realistic edge cases.

## Next

- Add Unix Maven Wrapper and Java 26 CI.
- Add a license after the owner selects MIT or Apache-2.0.
- Update contest documentation and demo only after behavior is stable.

## Verification

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-26.0.1.8-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean verify
```

Latest verification after the first hardening slice: 33 tests, zero failures, Java 26.0.1.
