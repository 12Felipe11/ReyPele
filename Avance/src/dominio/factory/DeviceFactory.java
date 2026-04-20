package dominio.factory;

import infraestructura.IHardwareComm;

/**
 * Fabrica abstracta para crear la capa de hardware.
 * Permite intercambiar entre simulador y Arduino real (HU-13).
 */
public abstract class DeviceFactory {
    public abstract IHardwareComm createHardwareComm();
}
