# Normaliser Library

A small Java library that maps free-text job titles onto a known list of job titles using fuzzy string matching.

```java
normaliser.normalize("Senior Software-Engineer!"); // -> "software engineer"
normaliser.normalize("Acountant");                 // -> "accountant"
```

## Requirements

- Java 25
- Maven (wrapper included)
- Spring Boot 4.1.1

## Build & test

```bash
./mvnw clean install
./mvnw test
```

## Usage

### Plain Java

```java
// Uses the default job titles
Normaliser normaliser = new Normaliser(null);

// Or supply your own
Normaliser custom = new Normaliser(List.of("Plumber", "Electrician", "Data Scientist"));

String title = normaliser.normalize("Lead Architect"); // "architect"
```

### Spring

`Normaliser` is a `@Component`, so it can be injected directly:

```java
@Service
public class MyService {
    private final Normaliser normaliser;

    public MyService(Normaliser normaliser) {
        this.normaliser = normaliser;
    }
}
```

With no job titles supplied, it falls back to the defaults.

> **Note:** Spring resolves the `List<String>` constructor parameter by collecting every `String` bean in the application context. If your application defines any `String` beans, they will be used as the job titles.

### Default job titles

- Architect
- Software Engineer
- Quantity Surveyor
- Accountant

## How it works

1. **Validate**: rejects `null`, blank input, and input containing digits.
2. **Clean**: lowercases the input, replaces non `a–z` characters with spaces, collapses whitespace and trims.
3. **Score**: compares the cleaned input against every job title. Each score is between 0 and 1:
    - An exact match scores `1.0`.
    - Otherwise the score is **50%** Jaro-Winkler similarity of the whole string, plus **50%** word-level similarity. The word-level part takes each word in the title, finds its best Jaro-Winkler match among the input words, and averages the results.
4. **Select**: the highest-scoring title is returned if its score is at least **0.65**. Otherwise an exception is thrown.

Jaro-Winkler is well suited to short strings. It tolerates typos and transpositions and gives extra weight to matching prefixes. The word-level score handles extra words (`"Senior"`, `"Lead"`) and differences in word order.

## Errors

Every failure throws an `InvalidInputException`, which is a `RuntimeException`:

| Input | Message |
|---|---|
| `null` | `input is null` |
| `""`, `"   "` | `input is empty` |
| `"Engineer 123"` | `input contains digits, was: Engineer 123` |
| `"!!!"`, `"这是中文"` | `input is empty after cleaning, was: ...` |
| `"Tech Lead"` | `input does not match any job title with sufficient confidence, was: 'Tech Lead', best match: 'architect' (56%)` |

Custom job titles passed to the constructor are validated in the same way.

## Limitations

- **Spelling, not meaning:** `"Developer"` does not match `"Software Engineer"` because the letters are not similar enough. Synonyms would need an alias map.
- **ASCII letters only:** accented and non-Latin characters are stripped (`"Ingénieur"` → `"ing nieur"`).
- **Digits are rejected:** for example, `"Level 2 Engineer"`.
- **Lowercase output:** titles are returned in their cleaned, lowercase form.
- **Fixed threshold:** the 0.65 confidence threshold is a constant and cannot be configured yet.
