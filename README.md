# Java Recipe Healthifier

![Recipe Healthifier cover](docs/assets/recipe-healthifier-cover.png)

A Java 26 command-line application that applies health-focused ingredient substitutions to recipes supplied as text files or recipe-page URLs.

## Modern Java 26

Recipe URL ingestion opts into the final HTTP/3 support delivered in Java 26 (JEP 517), with transparent fallback for servers that do not advertise HTTP/3. The project also uses records, sealed interfaces, pattern matching, text blocks, modules, immutable collection factories, `HttpClient`, and atomic NIO file operations—all without runtime dependencies.

## Build

Use a Java 26 JDK:

```powershell
.\mvnw.cmd clean verify
```

The executable JAR is created at `target/java-recipe-healthifier-1.0-SNAPSHOT.jar`.

## Recipe text format

```text
Chocolate Cake
Servings: 8

Ingredients:
- 2 cups all-purpose flour
- 1 cup sugar
- 1/2 cup vegetable oil

Instructions:
1. Mix the ingredients.
2. Bake until set.
```

`Ingredients` and one of `Instructions`, `Directions`, or `Steps` are required. `Servings` is optional.

## Convert recipes

From a local file:

```powershell
java -jar target/java-recipe-healthifier-1.0-SNAPSHOT.jar `
  --file recipe.txt `
  --rules KETO,GLUTEN_FREE,NO_SEED_OILS
```

From a webpage containing Schema.org Recipe JSON-LD:

```powershell
java -jar target/java-recipe-healthifier-1.0-SNAPSHOT.jar `
  --url "https://example.com/recipe" `
  --rules KETO,GLUTEN_FREE
```

Add a custom avoidance and save the result:

```powershell
java -jar target/java-recipe-healthifier-1.0-SNAPSHOT.jar `
  --file recipe.txt `
  --rules KETO `
  --avoid peanuts `
  --save
```

Supported rules are `NO_SEED_OILS`, `NO_UPF`, `KETO`, `LOW_CARB`, `CARNIVORE`, `GLUTEN_FREE`, `HIGH_PROTEIN`, and `DAIRY_FREE`.

## Recipe library

Saved recipes use `recipe-library.json` in the current directory by default. Select another location with `--library-file <path>`.

```powershell
# List saved recipes
java -jar target/java-recipe-healthifier-1.0-SNAPSHOT.jar --library

# Display one saved recipe
java -jar target/java-recipe-healthifier-1.0-SNAPSHOT.jar --show <entry-id>

# Delete one saved recipe
java -jar target/java-recipe-healthifier-1.0-SNAPSHOT.jar --delete <entry-id>
```

Run `java -jar target/java-recipe-healthifier-1.0-SNAPSHOT.jar --help` for the complete CLI syntax.

## Use from a phone

Start the dependency-free local web interface:

```powershell
java -jar target/java-recipe-healthifier-1.0-SNAPSHOT.jar --serve 8080
```

The terminal prints localhost and LAN addresses. Open the LAN address on a phone connected to the same trusted Wi-Fi network. Recipes remain on the computer running the JAR. Press `Ctrl+C` to stop the server.

## Contest documentation

- [Hackster submission story](docs/HACKSTER_SUBMISSION.md)
- [90–120 second demo script](docs/DEMO_SCRIPT.md)
- [Submission checklist](docs/SUBMISSION_CHECKLIST.md)
- [Ready-to-use demo recipe](examples/contest-demo-recipe.txt)
