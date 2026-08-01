# Demo video script (90–120 seconds)

Target length: approximately 105 seconds. Record at 1080p with the terminal text enlarged and notifications hidden.

## 0:00–0:12 — Problem and title

Show the cover image.

> This is Recipe Healthifier, a Java 26 application for adapting everyday recipes to household food preferences. Instead of manually checking every ingredient, I can select rules and receive clear, reviewable substitutions.

## 0:12–0:27 — Java 26 and build

Show `pom.xml` at the Java 26 compiler configuration, then run `java -version` and show the end of `mvnw clean verify`.

> The modular Maven project explicitly targets Java 26. It uses records, sealed interfaces, pattern matching, text blocks, the module system, and Java's HTTP and file APIs. The automated suite verifies the complete workflow.

## 0:27–0:42 — Input recipe

Show `recipe.txt` with flour, sugar, vegetable oil, and two short steps.

> Here is a familiar cake recipe. I want it gluten-free, keto-oriented, and free of seed oils.

## 0:42–1:05 — Convert

Run:

```powershell
java -jar target/java-recipe-healthifier-1.0-SNAPSHOT.jar --file recipe.txt --rules KETO,GLUTEN_FREE,NO_SEED_OILS --save
```

Scroll slowly through the output.

> The app changes all-purpose flour to almond flour, sugar to allulose, and vegetable oil to avocado oil. Every modified line identifies its source, while the instructions and servings remain intact. The compliance report summarizes the selected rules.

## 1:05–1:22 — URL ingestion and architecture

Briefly show `HttpRecipeSourceReader.java` and `TextRecipeConversionService.java`, or the architecture diagram in the submission story.

> Recipes can also be imported from webpages through Schema.org JSON-LD. HTTP and parsing are adapters around a deterministic conversion core, so tests run without relying on live websites.

## 1:22–1:40 — Local library

Copy the saved ID, then run `--library` and `--show <id>`.

> Saved conversions live in a local JSON library. They can be listed, reopened, or deleted, with atomic writes protecting the store. No account, API key, or cloud service is required.

## 1:40–1:48 — Close

Return to the cover image.

> Recipe Healthifier makes healthier home cooking less repetitive while keeping every decision visible and under the cook's control.

## Recording checklist

- Keep the finished video between 90 and 120 seconds.
- Show `java -version` reporting Java 26.
- Show a successful build or test result.
- Demonstrate conversion end to end.
- Avoid copyrighted music, logos, browser tabs, notifications, and personal data.
- Use only the original cover image and your own screen recording.
