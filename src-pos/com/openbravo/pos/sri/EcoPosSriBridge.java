package com.openbravo.pos.sri;

/**
 * Contrato minimo entre ECOPos y ecopos-sri-connector cuando este ultimo
 * corre fusionado en el mismo proceso/JVM (en vez de como servicio de
 * Windows separado). Solo tipos JDK en las firmas a proposito: esta misma
 * interfaz se compila tambien dentro del jar sombreado del conector
 * (com.openbravo.pos.sri.EcoPosSriBridgeImpl la implementa alli), y como el
 * {@code URLClassLoader} que carga ese jar en tiempo de ejecucion usa como
 * padre el classloader de ECOPos, esta copia (la de ECOPos) es siempre la
 * que gana la identidad de tipo para el cast - la copia del lado del
 * conector solo existe para que ese modulo compile por su cuenta. Mantener
 * ambas copias identicas byte a byte.
 *
 * Ver EcoPosSriGlue para como se instancia una implementacion de esta
 * interfaz.
 */
public interface EcoPosSriBridge {

    /** Procesa un ticket ya cerrado de forma asincrona (no bloquea al llamador). */
    void procesarTicketAsync(String ticketId);

    /** Abre la pantalla de configuracion del emisor (datos SRI + certificado digital). */
    void abrirConfiguracionEmisor();

    /** Abre la pantalla de configuracion de correo (SMTP). */
    void abrirConfiguracionCorreo();

    /** Abre el historial de facturacion (incluye anulacion/reintento/envio por correo desde ahi mismo). */
    void abrirHistorial();

    /** Arranca (si no estaba arrancado) el reintento periodico de comprobantes en ERROR/ENVIADO. */
    void iniciarReintentosPeriodicos();

    /** Detiene el reintento periodico. */
    void detenerReintentosPeriodicos();

    /** Libera los recursos del puente (executor, conexion dedicada). Llamar una sola vez, al cerrar ECOPos. */
    void cerrar();
}
