# Demo video script (90–120 seconds)

Target length: approximately 110 seconds. Record at 1080p, enlarge terminal text, hide notifications, and keep the phone and computer on the same trusted Wi-Fi network.

## 0:00–0:10 — Problem and title

Show the cover image, then a phone in the kitchen.

> Recipe Healthifier is a local Java 26 application that makes dietary recipe changes visible and reviewable instead of hiding them behind a cloud service.

## 0:10–0:24 — Verified Java 26 build

Show `java -version`, the `<release>26</release>` line in `pom.xml`, and the green Java 26 GitHub Actions check.

> The modular project compiles and tests on Java 26 on both Windows and Linux. It uses Java 26's final HTTP/3 client support, immutable records, sealed types, pattern matching, virtual threads, modules, and no runtime dependencies.

## 0:24–0:36 — Start the local interface

Run:

```powershell
java -jar target/java-recipe-healthifier-1.0-SNAPSHOT.jar --serve 8080
```

Show the printed LAN address, then open it on the phone.

> Java's built-in HTTP server makes the same conversion engine available on my phone. Recipe text stays on this computer, with no account or API key.

## 0:36–1:03 — Convert end to end

Paste `examples/contest-demo-recipe.txt`, select `KETO`, `GLUTEN_FREE`, and `NO_SEED_OILS`, then submit.

> The app replaces flour, sugar, and vegetable oil using the best compatible rules. It also updates matching instruction text, explains each applied swap, and computes compliance from the final ingredient list.

Pause on the converted ingredients, instructions, and three `COMPLIANT` statuses.

## 1:03–1:20 — Demonstrate honest limits

Paste `examples/unresolved-rice-bowl.txt` and select `KETO`:

```text
Rice Bowl
Ingredients:
- 2 cups white rice
Instructions:
1. Cook the white rice.
```

> When the rule vocabulary recognizes a violation but has no safe automatic swap, the app says `NOT POSSIBLE` and lists what needs attention. It never paints an unresolved recipe green.

## 1:20–1:38 — Architecture and Java 26

Show the architecture diagram, `HttpRecipeSourceReader.java` at `HTTP_3`, and `TextRecipeConversionService.java`.

> HTTP and the phone interface are adapters around a deterministic conversion core. URL imports prefer the HTTP/3 API finalized in Java 26, while tests exercise conversion without live websites.

## 1:38–1:50 — Close

Return to the phone result and cover image.

> Recipe Healthifier makes healthier home cooking less repetitive while keeping limitations, data, and decisions under the cook's control.

## Recording checklist

- Keep the finished video between 90 and 120 seconds.
- Show `java -version` reporting Java 26 and the green CI result.
- Show the LAN URL and the working interface on a phone.
- Demonstrate both a successful conversion and an honest unresolved result.
- Avoid copyrighted music, logos, unrelated browser tabs, notifications, and personal data.
- Use only the original cover image and your own screen recording.
