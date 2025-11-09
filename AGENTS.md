# AGENTS.md

## Zweck der Anwendung
CampusCoffee stellt eine REST\-API bereit, mit der externe Clients Points of Sale (POS) verwalten können. Clients können POS auflisten, per ID abrufen, neu anlegen (per JSON) sowie über einen OpenStreetMap\-Node importieren und bestehende Einträge aktualisieren. Die Anwendung ist für den Betrieb mit PostgreSQL vorgesehen und bietet im `dev`\-Profil initiale Daten sowie eine einfache Startkonfiguration.

## Projektstruktur und Architektur
\- Multi\-Modul\-Maven\-Projekt mit folgenden Modulen (Verzeichnisse vorhanden):
\- `api`: Bereitstellung der REST\-Schnittstelle.  
\- `application`: Ausführung der Anwendung, Konfiguration, Start; Ressourcen unter `application/src/main/resources` (z.\,B. `application/src/main/resources/application.yaml`).  
\- `domain`: Domänenmodell und Kernkonzepte.  
\- `data`: Persistenzschicht und Datenzugriff.  
\- `doc/prp`: Projektdokumentationsvorlagen.  
\- Endpunkte laut `README.md`:
\- `GET /api/pos`, `GET /api/pos/{id}`  
\- `POST /api/pos`  
\- `POST /api/pos/import/osm/{nodeId}`  
\- `PUT /api/pos/{id}`

## Build, Test und Start
\- Build: `mvn clean install` (siehe `README.md`).  
\- Tests: werden im Maven\-Build ausgeführt; alternativ `mvn test`.  
\- Start (dev):  
\- PostgreSQL starten (siehe `README.md`).  
\- `cd application` und `mvn spring-boot:run -Dspring-boot.run.profiles=dev`.  
\- Konfiguration: `application/src/main/resources/application.yaml`.

## Richtlinien für KI\-Agents
\- Architektur respektieren: Trenne Verantwortlichkeiten zwischen `api` (Controller/REST), `application` (Orchestrierung/Start/Config), `domain` (Domänenmodell) und `data` (Persistenz).  
\- Controller vs. Service vs. Domain klar trennen: HTTP/DTO/Validierung in `api`, Fachlogik in `application` (Service\-Ebene), Modell in `domain`, Datenzugriff in `data`.  
\- Tests beachten: Vorhandene Tests nicht brechen; neue Funktionalität mit Unit\-/Integrationstests absichern.  
\- Keine großflächigen Refactorings, keine neuen Frameworks oder Build\-Tools ohne Notwendigkeit.  
\- Orientiere dich an den bestehenden Packages, Klassen und Konventionen dieses Repositories.
Wenn du neue Methoden, Klassen oder Dateien vorschlägst, platziere sie konsistent zur vorhandenen Struktur
und kennzeichne klar, dass sie neu angelegt werden sollen.
Erfinde keine Pfade oder Abhängigkeiten, die nicht in das bestehende Projekt passen.
\- Ergänzungen klar kennzeichnen:
\- Beispiel: „neue Komponente für OSM\-Abruf im Modul `application` (neu anzulegen)“.  
\- Beispiel: „Konfigurationseintrag in `application/src/main/resources/application.yaml` ergänzen (neu anzulegen, falls Schlüssel noch fehlt)“.

## Hinweise zu neuen Vorschlägen (neu anzulegen)
\- OSM\-Import intern kapseln: eigenständige Komponente im Modul `application` für HTTP\-Abruf und Parsing von OSM (neu anzulegen).  
\- Mapping von OSM\-Feldern in die Domäne in bestehende Struktur integrieren; falls zusätzliche Domänenattribute oder Persistenzfelder erforderlich sind, minimalinvasiv im Modul `domain` bzw. `data` ergänzen (neu anzulegen, falls nicht vorhanden).  
\- Konfiguration für OSM\-Basis\-URL als Property in `application/src/main/resources/application.yaml` ergänzen (neu anzulegen, falls Schlüssel fehlt).

## Referenzen
\- `README.md` (Build, Start, API\-Beispiele)  
\- `CHANGELOG.md` (Änderungshistorie)  
\- `application/src/main/resources/application.yaml` (Konfiguration)
