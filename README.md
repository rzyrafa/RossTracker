<div align="center">

# 🛒 RossTracker

**Śledź ceny produktów z Rossmanna i nie przegap żadnej promocji.**

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Wersja](https://img.shields.io/badge/wersja-1.4.1-8B5E3C)

</div>

---

RossTracker to natywna aplikacja na Androida, która pilnuje cen wybranych produktów
z [rossmann.pl](https://www.rossmann.pl). Wklejasz link (albo udostępniasz go prosto
ze sklepu), a aplikacja dwa razy dziennie sprawdza cenę, zapisuje historię i wysyła
powiadomienie, gdy zaczyna się promocja lub cena spada.

## ✨ Funkcje

- **Dodawanie produktów linkiem** – wklej adres produktu albo użyj „Udostępnij → RossTracker” w przeglądarce.
- **Automatyczne sprawdzanie cen** o 7:00 i 19:00 (WorkManager, tylko przy dostępie do sieci, z ponawianiem przy błędach).
- **Powiadomienia** o starcie promocji (z procentem rabatu i datą końca) oraz o zwykłych obniżkach – bez dubli.
- **Historia ceny na wykresie** dla każdego produktu.
- **Etykiety** z kolorami – grupuj produkty, zaznaczaj wiele naraz i filtruj listę.
- **Jasny i ciemny motyw** w kawowej kolorystyce, czcionki Inter i Playfair Display.
- **Skrót na ekranie głównym** do szybkiego dodawania produktu.
- **Dane tylko lokalnie** – baza Room na telefonie, bez kont i bez serwera.

## 🧱 Stos technologiczny

| Obszar | Biblioteki |
| --- | --- |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Dane | Room (z eksportem schematów), Kotlinx Serialization |
| Sieć i parsowanie | OkHttp, jsoup |
| Zadania w tle | WorkManager |
| Obrazki i wykresy | Coil 3, Vico |
| Testy | JUnit 4 |

## 📁 Struktura projektu

```
app/src/main/java/pl/rosstracker/
├── data/
│   ├── db/            # Room: encje, DAO, AppDatabase
│   ├── net/           # RossmannClient (HTTP) i RossmannParser (HTML → cena)
│   ├── PriceEvents.kt # wykrywanie promocji i obniżek między sprawdzeniami
│   └── *Repository.kt
├── notify/            # kanały i treść powiadomień
├── ui/
│   ├── list/          # lista produktów + filtrowanie
│   ├── detail/        # szczegóły produktu i wykres ceny
│   ├── add/           # dialog dodawania produktu
│   ├── labels/        # zarządzanie etykietami
│   └── common/        # wspólne komponenty (PriceTag, animacje)
└── work/              # harmonogram i worker sprawdzający ceny
```

## 🚀 Budowanie

Wymagania: **Android Studio** (lub sam Android SDK) i **JDK 17+**.

```bash
git clone https://github.com/rzyrafa/RossTracker.git
cd RossTracker
./gradlew assembleDebug
```

Gotowy plik APK znajdziesz w `app/build/outputs/apk/debug/`.

### Wersja release (podpisana)

Dane do podpisu **nie są w repozytorium** – dodaj je do `~/.gradle/gradle.properties`:

```properties
ROSSTRACKER_STORE_FILE=/ścieżka/do/keystore.jks
ROSSTRACKER_STORE_PASSWORD=...
ROSSTRACKER_KEY_ALIAS=...
ROSSTRACKER_KEY_PASSWORD=...
```

```bash
./gradlew assembleRelease
```

Dostępny jest też wariant `staging` – szybkość release'u, klucz debug i osobny identyfikator
aplikacji (`pl.rosstracker.staging`), więc można go mieć obok wersji produkcyjnej.

## 🧪 Testy

```bash
./gradlew test
```

Testy jednostkowe obejmują parser stron produktów (na zapisanych przykładowych HTML-ach),
wykrywanie zdarzeń cenowych, filtrowanie listy i wyliczanie godzin sprawdzeń.

## ⚠️ Uwaga

Projekt hobbystyczny, niezwiązany z Rossmann SDP. Aplikacja odczytuje publiczne strony produktów
i działa tylko tak długo, jak ich struktura się nie zmieni.

Czcionki Inter i Playfair Display są na licencji SIL Open Font License (`app/src/main/assets/licenses/`).
