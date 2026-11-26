package dispositivo.controladores;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONException;
import org.json.JSONObject;

import dispositivo.interfaces.Configuracion;
import dispositivo.interfaces.IDispositivo;
import dispositivo.utils.MySimpleLogger;

public class ControladorSemaforico {

	protected MqttClient myClient;
	protected MqttConnectOptions connOpt;

	protected String mqttBroker = null;
	protected String dispositivo1 = null;
	protected String dispositivo2 = null;

	private String loggerId = "ControladorSemaforico";


	public static void main(String[] args) {

		if ( args.length < 3 ) {
			System.out.println("Usage: java ControladorSemaforico mqttBroker device1 device2");
			System.out.println("Example: java ControladorSemaforico tcp://ttmi008.iot.upv.es:1883 ttmi050 ttmi051");
			return;
		}

		String mqttBroker = args[0];
		String dispositivo1 = args[1];
		String dispositivo2 = args[2];

		ControladorSemaforico controlador = new ControladorSemaforico(mqttBroker, dispositivo1, dispositivo2);
		controlador.connect();
		controlador.iniciar();
	}

	protected ControladorSemaforico(String mqttBroker, String dispositivo1, String dispositivo2) {
		this.mqttBroker = mqttBroker;
		this.dispositivo1 = dispositivo1;
		this.dispositivo2 = dispositivo2;
	}


	public void connect() {

		// setup MQTT Client
		String clientID = "controlador" + UUID.randomUUID().toString();
		connOpt = new MqttConnectOptions();

		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);

		// Connect to Broker
		try {

			MqttDefaultFilePersistence persistence = null;
			try {
				persistence = new MqttDefaultFilePersistence("/tmp");
			} catch (Exception e) {
			}
			if ( persistence != null )
				myClient = new MqttClient(this.mqttBroker, clientID, persistence);
			else
				myClient = new MqttClient(this.mqttBroker, clientID);

			myClient.connect(connOpt);
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		MySimpleLogger.info(this.loggerId, "Conectado al broker " + this.mqttBroker);

	}


	public void iniciar() {
		int tiempoEspera = 3;

		//   1. Pongo en rojo ambos semáforos
		encenderLuzApagarOtras(dispositivo1, "f1");
		encenderLuzApagarOtras(dispositivo2,"f1" );

		while(true) {
			//   2. Pongo en verde al dispositivo 1 durante un tiempo
			encenderLuzApagarOtras(dispositivo1, "f3");
			esperar(tiempoEspera);

			//   3. Pongo en amarillo al dispositivo 1, durante un tiempo
			encenderLuzApagarOtras(dispositivo1, "f2");
			esperar(tiempoEspera);

			//   4. Pongo en rojo al dispositivo 1, durante un tiempo
			encenderLuzApagarOtras(dispositivo1, "f1");
			esperar(tiempoEspera);

			//   5. Pongo en verde al dispositivo 2, durante un tiempo
			encenderLuzApagarOtras(dispositivo2, "f3");
			esperar(tiempoEspera);

			//   6. Pongo en amarillo al dispositivo 2, durante un tiempo
			encenderLuzApagarOtras(dispositivo2, "f2");
			esperar(tiempoEspera);

			//   7. Pongo en rojo al dispositivo 2, durante un tiempo
			encenderLuzApagarOtras(dispositivo2, "f1");
			esperar(tiempoEspera);
		}
	}

	protected void encenderLuzApagarOtras(String dispositivo, String funcionId) {
		Set<String> funciones = new HashSet<>();
		funciones.add("f1");
		funciones.add("f2");
		funciones.add("f3");
		funciones.remove(funcionId);

		for (String funcion : funciones) {
			enviarAccion(dispositivo, funcion, "apagar");
		}
		enviarAccion(dispositivo, funcionId, "encender");
	}


	// Envía una acción (encender, apagar o parpadear) a una función de un dispositivo
	protected void enviarAccion(String dispositivoId, String funcionId, String accion) {

		String topic = Configuracion.TOPIC_BASE + "dispositivo/" + dispositivoId + "/funcion/" + funcionId + "/comandos";

		JSONObject pubMsg = new JSONObject();
		try {
			pubMsg.put("accion", accion);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		MqttMessage message = new MqttMessage(pubMsg.toString().getBytes());
		message.setQos(0);
		message.setRetained(false);

		try {
			myClient.publish(topic, message);
			MySimpleLogger.debug(this.loggerId, topic + " " + pubMsg.toString());
		} catch (MqttException e) {
			e.printStackTrace();
		}

	}


	protected void esperar(int segundos) {
		try {
			Thread.sleep(segundos * 1000);
		} catch (InterruptedException e) {
		}
	}

}
