# Engineering decisions

## Honest compliance semantics

- `COMPLIANT`: no known violation remains, and any evidence required by the rule is present.
- `PARTIAL`: the conversion improved the requested rule but a known violation remains, or a positive goal such as high protein lacks enough evidence.
- `NOT_POSSIBLE`: one or more known violations remain and no applicable substitution was made.

Compliance is vocabulary-based and must be described as such; it is not medical or nutritional certification.

## Rules versus applied swaps

`Swap` remains an immutable record of a substitution actually presented to the user. A separate `HealthRule` describes detection vocabulary, and `SwapRule` describes reusable substitution behavior across one or more categories.

## Java 26

Use final Java 26 functionality where it naturally improves the product. Prefer HTTP/3 (JEP 517). Do not force preview APIs into the submission merely to increase the feature count because they require `--enable-preview` at runtime.
