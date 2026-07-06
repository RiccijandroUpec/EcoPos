# 🌱 EcoPos

![Java](https://img.shields.io/badge/Java-11-ED8B00?logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/UI-Java%20Swing-red)
![MySQL](https://img.shields.io/badge/MySQL%2FMariaDB-4479A1?logo=mysql&logoColor=white)
![JasperReports](https://img.shields.io/badge/Reports-JasperReports-orange)
![Ant](https://img.shields.io/badge/Build-Apache%20Ant-A81C7D?logo=apacheant&logoColor=white)
![License](https://img.shields.io/badge/License-GPLv3-blue.svg)

EcoPos es un sistema de Punto de Venta (POS) de escritorio para negocios de retail y hostelería, construido en Java Swing.

Licenciado bajo [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.html).

## ✨ Características

- 🖱️ Pantalla de ventas táctil con categorías, productos y botones personalizables
- 👥 Roles multiusuario (Administrador, Gerente, Empleado, Invitado) con permisos
- 🧾 Impresión de tickets/recibos con plantillas personalizables (JasperReports)
- 📦 Gestión de inventario, clientes, proveedores e impuestos
- 🗄️ Compatible con MySQL/MariaDB, PostgreSQL, Oracle y bases de datos embebidas Derby/HSQLDB
- 📠 Integración con lector de código de barras, cajón de dinero e impresora de tickets (JavaPOS)

## ⚙️ Requisitos

| Herramienta | Uso | Enlace |
|---|---|---|
| ![Java](https://img.shields.io/badge/-Java%2011-ED8B00?logo=openjdk&logoColor=white) | Ejecutar y compilar la app | [Adoptium Temurin 11](https://adoptium.net/temurin/releases/?version=11) |
| ![MariaDB](https://img.shields.io/badge/-MariaDB%2FMySQL-4479A1?logo=mysql&logoColor=white) | Base de datos (recomendada) | [XAMPP](https://www.apachefriends.org/) · [MariaDB](https://mariadb.org/) |
| ![Ant](https://img.shields.io/badge/-Apache%20Ant-A81C7D?logo=apacheant&logoColor=white) | Build original del proyecto | [ant.apache.org](https://ant.apache.org/) |
| ![JasperReports](https://img.shields.io/badge/-JasperReports-F28E1C) | Motor de tickets/reportes | [community.jaspersoft.com](https://community.jaspersoft.com/) |

Compatible con Windows, Linux o macOS.

## 📁 Estructura del proyecto

| Ruta | Contenido |
|---|---|
| `src-pos/` | Código fuente principal de la aplicación (`com.openbravo.pos.*`) |
| `src-beans/` | Componentes Swing reutilizables |
| `src-data/` | Capa de acceso a datos (`com.openbravo.data.*`) |
| `lib/` | Dependencias de terceros (`.jar`) incluidas en el repo |
| `locales/` | Traducciones de la interfaz (~90 idiomas) |
| `reports/` | Plantillas JasperReports para tickets y reportes |
| `build_working.xml` | Script de build Ant autocontenido (compila y empaqueta el jar) |

> 💡 Los paquetes Java internos usan el namespace `com.openbravo.*`.

## 🔨 Compilación

El `build.xml` original (NetBeans + Ant) depende de metadatos `nbproject/` que no están en este repositorio. En su lugar, usa **`build_working.xml`**, un build Ant autocontenido que sí compila y empaqueta el proyecto de punta a punta:

```sh
# Instala Apache Ant si no lo tienes (https://ant.apache.org/bindownload.cgi)
ant -f build_working.xml jar
```

Esto genera `build/jar/ecopos.jar`. Si no tienes Ant a mano, el equivalente manual con solo el JDK es:

```sh
# Desde la raíz del proyecto
mkdir -p build/classes

# Compilar los tres módulos fuente juntos (se referencian entre sí)
find src-beans src-data src-pos -name "*.java" > sources.txt
javac -encoding UTF-8 -d build/classes -cp "lib/*" \
  -sourcepath "src-beans;src-data;src-pos" @sources.txt

# Copiar recursos no-Java (íconos, .properties, etc.) al directorio de clases
for d in src-beans src-data src-pos; do
  (cd "$d" && find . -type f ! -name "*.java" ! -name "*.form" -print0 \
    | tar --null -T - -cf -) | (cd build/classes && tar -xf -)
done

# Empaquetar el jar ejecutable
mkdir -p build/jar
printf 'Main-Class: com.openbravo.pos.forms.StartPOS\n' > manifest.txt
jar cfm build/jar/ecopos.jar manifest.txt -C build/classes .
```

> 💡 `src-beans`, `src-data` y `src-pos` se referencian entre sí (por ejemplo, componentes en `src-beans` usan `com.openbravo.pos.forms.AppConfig`), así que cualquier compilación necesita ver los tres directorios en el `sourcepath`, y cualquiera de los tres módulos puede terminar necesitando `lib/*.jar` (p. ej. `RXTXcomm.jar` para el soporte de puerto serie) — por eso las tres reglas de compilación en `build_working.xml` comparten el mismo classpath.

## ▶️ Ejecución

Ejecuta el jar con las librerías necesarias en el classpath (ver `start.bat` / `start.sh` para la lista completa — JasperReports, POI, iText, Substance L&F, el driver JDBC de tu base de datos, etc.):

```sh
java -cp "build/jar/ecopos.jar;lib/jasperreports-4.5.1.jar;lib/jcommon-1.0.15.jar;lib/jfreechart-1.0.12.jar;lib/swing-layout-1.0.4.jar;lib/AbsoluteLayout.jar;lib/trident.jar;lib/substance.jar;lib/substance-swingx.jar;lib/substance-extras.jar;lib/swingx-all-1.6.4.jar;lib/mysql-connector-java-5.1.49.jar;locales/;reports/" \
  -Ddirname.path="./" com.openbravo.pos.forms.StartPOS
```

O más simple, usa el script ya armado con el classpath completo (todos los idiomas incluidos):

```sh
./start.sh        # Linux/macOS
start.bat         # Windows
```

Al primer arranque, EcoPos escribe su configuración en `~/ecopos.properties`. Por defecto apunta a una base de datos Derby embebida; edita ese archivo (o usa la pantalla **Configuración → Base de datos** dentro de la app) para apuntar a MySQL/MariaDB, PostgreSQL, etc. Si apunta a un esquema vacío, EcoPos crea automáticamente todas las tablas y datos iniciales (roles, categoría/producto/impuestos por defecto) en el siguiente arranque.

> 💡 `ResourceBundle` solo busca archivos de traducción en la raíz del classpath, no en subcarpetas — por eso `start.bat`/`start.sh` agregan explícitamente `locales/<Idioma>/locales/` y `locales/<Idioma>/reports/` de los 15 idiomas incluidos. Si armas tu propio classpath a mano (como el comando de arriba), sin esas rutas la app cae siempre a inglés sin importar `user.language`.

## ✅ Tests

Hay un puñado de tests JUnit para las clases de lógica pura (sin GUI ni base de datos): `AltEncrypter` (cifrado ida y vuelta), `LuhnAlgorithm` (validación de tarjetas) y `StringUtils`.

```sh
ant -f build_working.xml test
```

## 🗄️ Datos por defecto

Una base de datos nueva viene con:

- 👤 Cuatro roles: Administrador, Gerente, Empleado, Invitado
- 🏷️ Una categoría por defecto (`CATEGORY STANDARD`) y un producto (`xxx999`)
- 💵 Dos tasas de impuesto: Exenta y Estándar

Renómbralos según tu negocio — **no los elimines**, otros registros pueden depender de sus IDs.

## 🔗 Enlaces importantes

- 📦 Repositorio: [github.com/RiccijandroUpec/EcoPos](https://github.com/RiccijandroUpec/EcoPos)
- 📜 Licencia GPL v3: [gnu.org/licenses/gpl-3.0](https://www.gnu.org/licenses/gpl-3.0.html)
- ☕ Java 11 (Temurin): [adoptium.net](https://adoptium.net/temurin/releases/?version=11)
- 🐘 XAMPP (MariaDB/MySQL local): [apachefriends.org](https://www.apachefriends.org/)
- 🧾 JasperReports: [community.jaspersoft.com](https://community.jaspersoft.com/)

## 📜 Licencia

GNU GPL v3 — ver las cabeceras de licencia en los archivos fuente individuales.
