# Contest submission checklist

Hackster's registration email confirms that project submissions close August 16, 2026 at 11:59 PM PT. Save that email with the project records because parts of the rules page contain older, conflicting dates.

## Completed in the repository

- [x] Health-focused Java project
- [x] Maven compiler explicitly targets Java 26
- [x] End-to-end text recipe conversion
- [x] URL recipe ingestion
- [x] Local JSON recipe library
- [x] Executable JAR configuration
- [x] Automated test suite
- [x] Project name and short description
- [x] Bill of materials
- [x] Full beginner instructions
- [x] Architecture explanation
- [x] Original cover image
- [x] 90–120 second demo script
- [x] English-language documentation

## Entrant actions required before submission

- [ ] Confirm every eligibility statement applies to you.
- [x] Obtain written deadline confirmation from Hackster: August 16, 2026 at 11:59 PM PT.
- [x] Create a public GitHub repository: <https://github.com/darcy0408/java-recipe-healthifier>
- [x] Review the repository for secrets, private information, and unintended third-party assets.
- [ ] Add an open-source license only if you are comfortable granting it.
- [x] Push the complete source, tests, Maven wrapper, `pom.xml`, README, and `docs` folder.
- [ ] Record a 90–120 second demo using `DEMO_SCRIPT.md`.
- [ ] Upload the demo video and verify its visibility.
- [ ] Capture at least two screenshots: successful conversion and saved-library listing.
- [ ] Create the Hackster project and paste/adapt `HACKSTER_SUBMISSION.md`.
- [ ] Upload `assets/recipe-healthifier-cover.png` as the cover.
- [ ] Add the screenshots and demo video to the project story.
- [ ] Add the public source repository link under resources/code.
- [ ] Confirm the submission preview contains the name, short description, BOM, instructions, images, video, and code link.
- [ ] Click the contest's final Review and Submit action before the applicable deadline.
- [ ] Save screenshots of the submitted/confirmation state.

## Recommended final smoke test

```powershell
java -version
.\mvnw.cmd clean verify
java -jar target\java-recipe-healthifier-1.0-SNAPSHOT.jar --help
```

Do not add OCR, a graphical interface, or new external integrations before submitting unless every required item above is already complete.
