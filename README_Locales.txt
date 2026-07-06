EcoPos - Locales / Translations
===============================

EcoPos ships with 15 languages. English is the default/fallback language;
not all locales are completely translated (untranslated keys automatically
fall back to English).

Application locale files:
- beans_messages.properties  - generic Java UI component messages
- data_messages.properties   - data-access layer messages
- erp_messages.properties    - legacy ERP-integration labels
- pos_messages.properties    - the MAIN application labels and messages

Reports:
Each report has its own .properties file, found under
reports/com/openbravo/reports/

Directory layout:
- locales/<Language>/locales/   - that language's *_xx.properties files
- locales/<Language>/reports/   - that language's report label files

How language selection works:
start.bat and start.sh already put every bundled language's locales/ and
reports/ subfolder on the classpath, so switching language is just a matter
of setting it in EcoPos Configuration > Locale and restarting - no manual
file copying needed.

If you build your own classpath by hand (skipping start.bat/start.sh), note
that Java's ResourceBundle only looks for translation files directly on the
classpath root, not in subfolders. Without explicitly adding
locales/<Language>/locales/ and locales/<Language>/reports/ to your
classpath, the app will silently fall back to English regardless of the
configured language.
