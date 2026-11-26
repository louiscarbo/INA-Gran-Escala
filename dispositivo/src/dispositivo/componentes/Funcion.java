package dispositivo.componentes;

import dispositivo.api.mqtt.FuncionPublisher_MQTT;
import dispositivo.interfaces.FuncionStatus;
import dispositivo.interfaces.IFuncion;
import dispositivo.utils.MySimpleLogger;

public class Funcion implements IFuncion {
	
	protected String id = null;
	protected FuncionStatus initialStatus = null;
	protected FuncionStatus status = null;
	protected boolean habilitado = true;
	protected FuncionPublisher_MQTT publisher = null;
	
	private String loggerId = null;
	
	public static Funcion build(String id) {
		return new Funcion(id, FuncionStatus.OFF);
	}
	
	public static Funcion build(String id, FuncionStatus initialStatus) {
		return new Funcion(id, initialStatus);
	}

	protected Funcion(String id, FuncionStatus initialStatus) {
		this.id = id;
		this.initialStatus = initialStatus;
		this.loggerId = "Funcion " + id;
	}
	
	
	@Override
	public String getId() {
		return this.id;
	}
		
	@Override
	public IFuncion encender() {
		if ( !this.estaHabilitada() ) {
			MySimpleLogger.warn(this.loggerId, "Funcion deshabilitada, no se puede modificar");
			return this;
		}
		MySimpleLogger.info(this.loggerId, "==> Encender");
		this.setStatus(FuncionStatus.ON);
		// Ejercicio 9 - Publicar estado de Funcion
		this.publishStatusChange();
		return this;
	}

	@Override
	public IFuncion apagar() {
		if ( !this.estaHabilitada() ) {
			MySimpleLogger.warn(this.loggerId, "Funcion deshabilitada, no se puede modificar");
			return this;
		}
		MySimpleLogger.info(this.loggerId, "==> Apagar");
		this.setStatus(FuncionStatus.OFF);
		// Ejercicio 9 - Publicar estado de Funcion
		this.publishStatusChange();
		return this;
	}

	@Override
	public IFuncion parpadear() {
		if ( !this.estaHabilitada() ) {
			MySimpleLogger.warn(this.loggerId, "Funcion deshabilitada, no se puede modificar");
			return this;
		}
		MySimpleLogger.info(this.loggerId, "==> Parpadear");
		this.setStatus(FuncionStatus.BLINK);
		// Ejercicio 9 - Publicar estado de Funcion
		this.publishStatusChange();
		return this;
	}
	
	protected IFuncion _putIntoInitialStatus() {
		switch (this.initialStatus) {
		case ON:
			this.encender();
			break;
		case OFF:
			this.apagar();
			break;
		case BLINK:
			this.parpadear();
			break;

		default:
			break;
		}
		
		return this;

	}

	@Override
	public FuncionStatus getStatus() {
		return this.status;
	}
	
	protected IFuncion setStatus(FuncionStatus status) {
		this.status = status;
		return this;
	}
	
	@Override
	public IFuncion iniciar() {
		this._putIntoInitialStatus();
		return this;
	}
	
	@Override
	public IFuncion detener() {
		return this;
	}
	
	
	public boolean estaHabilitada() {
		return habilitado;
	}

	public void habilitar() {
		this.habilitado = true;
	}

	public void deshabilitar() {
		this.habilitado = false;
	}

	public void setPublisher(FuncionPublisher_MQTT publisher) {
		this.publisher = publisher;
	}

	protected void publishStatusChange() {
		
		// Ejercicio 9 - Implementar notificaciones 'push'
		if ( this.publisher != null )
			this.publisher.publish_status();

	}
}
