package com.openbravo.pos.sri;

import com.openbravo.pos.forms.AppProperties;
import com.openbravo.pos.util.AltEncrypter;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Carga ecopos-sri-connector (un modulo Maven separado, jar sombreado) en el
 * mismo proceso/JVM que ECOPos, sin agregarlo nunca al classpath propio de
 * ECOPos (start.bat no cambia): el jar se ubica en tiempo de ejecucion desde
 * una ruta relativa fija y se carga con un {@link URLClassLoader} normal
 * (parent-first, padre = el classloader de ECOPos) - el conector ya reubica
 * (shade relocation) org.slf4j/ch.qos.logback en su propio jar para no
 * chocar con las versiones viejas que trae ECOPos, asi que no hace falta un
 * classloader aislante a medida.
 *
 * Si el jar no existe (SRI fusionado no instalado) o cualquier paso de esta
 * inicializacion falla (BD, reflexion, etc.),
 * {@link #getInstance(AppProperties)} devuelve {@code null} - un problema
 * aca nunca debe impedir que ECOPos mismo arranque.
 */
public final class EcoPosSriGlue {

    private static final Logger LOG = Logger.getLogger(EcoPosSriGlue.class.getName());

    private static final String CARPETA_CONECTOR_RELATIVA = "sri-conector";
    private static final String RUTA_JAR_RELATIVA = CARPETA_CONECTOR_RELATIVA + "/ecopos-sri-connector.jar";

    private static EcoPosSriBridge instancia;
    private static URLClassLoader classLoaderConector;
    private static boolean inicializado;

    private EcoPosSriGlue() {
    }

    /**
     * Arranca el puente (si el jar del conector esta instalado) y su
     * reintento periodico. Pensado para llamarse una vez al arrancar ECOPos
     * ({@code StartPOS}); nunca lanza.
     */
    public static synchronized void startAtBoot(AppProperties propiedades) {
        EcoPosSriBridge bridge = getInstance(propiedades);
        if (bridge != null) {
            bridge.iniciarReintentosPeriodicos();
        }
    }

    /** Devuelve el puente ya inicializado, o lo inicializa la primera vez. Null si no esta instalado o algo fallo. */
    public static synchronized EcoPosSriBridge getInstance(AppProperties propiedades) {
        if (inicializado) {
            return instancia;
        }
        inicializado = true;
        try {
            instancia = construir(propiedades);
            if (instancia != null) {
                Runtime.getRuntime().addShutdownHook(new Thread(EcoPosSriGlue::shutdown, "sri-conector-shutdown"));
            }
        } catch (Throwable e) {
            LOG.log(Level.WARNING, "No se pudo inicializar ecopos-sri-connector (modo fusionado) - la facturacion SRI queda desactivada", e);
            instancia = null;
        }
        return instancia;
    }

    private static EcoPosSriBridge construir(AppProperties propiedades) throws Exception {
        File dirname = new File(System.getProperty("dirname.path", "./"));
        File archivoJar = new File(dirname, RUTA_JAR_RELATIVA);
        if (!archivoJar.exists()) {
            LOG.info(() -> "No existe " + archivoJar + " - ecopos-sri-connector no esta instalado, se omite");
            return null;
        }

        Connection conexionDedicada = abrirConexionDedicada(propiedades);

        classLoaderConector = new URLClassLoader(
                new URL[]{archivoJar.toURI().toURL()},
                EcoPosSriGlue.class.getClassLoader());

        Path carpetaConector = new File(dirname, CARPETA_CONECTOR_RELATIVA).toPath();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "sri-conector-trabajo");
            hilo.setDaemon(true);
            return hilo;
        });

        Class<?> claseImpl = classLoaderConector.loadClass("com.openbravo.pos.sri.EcoPosSriBridgeImpl");
        Constructor<?> constructor = claseImpl.getConstructor(Connection.class, Path.class, ExecutorService.class);
        return (EcoPosSriBridge) constructor.newInstance(conexionDedicada, carpetaConector, executor);
    }

    /**
     * Conexion JDBC propia y dedicada para el conector (NO la de
     * {@code AppView.getSession()}): el procesamiento SRI corre en un hilo
     * de fondo (SOAP + espera real de varios segundos), y compartir la
     * misma conexion que el EDT no es seguro con el driver JDBC que usa
     * ECOPos. Mismas credenciales que ECOPos ya usa (AppConfig), descifrando
     * la clave igual que AppViewConnection.createSession.
     */
    private static Connection abrirConexionDedicada(AppProperties propiedades) throws Exception {
        String url = propiedades.getProperty("db.URL");
        String usuario = propiedades.getProperty("db.user");
        String clave = propiedades.getProperty("db.password");
        if (usuario != null && clave != null && clave.startsWith("crypt:")) {
            AltEncrypter cypher = new AltEncrypter("cypherkey" + usuario);
            clave = cypher.decrypt(clave.substring(6));
        }
        return DriverManager.getConnection(url, usuario, clave);
    }

    /** Libera los recursos del puente y del classloader dedicado. Registrado como shutdown hook de la JVM. */
    public static synchronized void shutdown() {
        if (instancia != null) {
            try {
                instancia.cerrar();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error cerrando ecopos-sri-connector", e);
            }
        }
        if (classLoaderConector != null) {
            try {
                classLoaderConector.close();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error cerrando el classloader de ecopos-sri-connector", e);
            }
        }
    }
}
