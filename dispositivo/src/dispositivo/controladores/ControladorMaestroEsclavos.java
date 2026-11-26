package dispositivo.controladores;

import java.util.Arrays;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONException;
import org.json.JSONObject;

import dispositivo.interfaces.Configuracion;
import dispositivo.utils.MySimpleLogger;

public class ControladorMaestroEsclavos implements MqttCallback {

	protected MqttClient myClient;
	protected MqttConnectOptions connOpt;

	protected String mqttBroker = null;
	protected String maestro = null;
	protected String[] esclavos = null;

	private String loggerId = "ControladorMaestroEsclavos";

	public static void main(String[] args) {

		if ( args.length < 3 ) {
			System.out.println("Usage: java ControladorMaestroEsclavos mqttBroker maestro esclavo1 esclavo2...");
			System.out.println("Example: java ControladorMaestroEsclavos tcp://ttmi008.iot.upv.es:1883 ttmi050 ttmi051...");
			return;
		}

		String mqttBroker = args[0];
		String maestro = args[1];
		String[] esclavos = Arrays.copyOfRange(args, 2, args.length);

		ControladorMaestroEsclavos controlador = new ControladorMaestroEsclavos(mqttBroker, maestro, esclavos);
		controlador.connect();
		controlador.iniciar();
	}

	protected ControladorMaestroEsclavos(String mqttBroker, String maestro, String[] esclavos) {
		this.mqttBroker = mqttBroker;
		this.maestro = maestro;
		this.esclavos = esclavos;
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

			myClient.setCallback(this);
			myClient.connect(connOpt);
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		MySimpleLogger.info(this.loggerId, "Conectado al broker " + this.mqttBroker);
	}

	public void iniciar() {
		this.subscribe(Configuracion.TOPIC_BASE + "dispositivo/" + maestro + "/funcion/f1/info");
	}

	// We receive {"estado":"ON","id":"f1"}
	// On topic dispositivo/{maestro_ID}/funcion/f1/info
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String payload = new String(message.getPayload());
		
		System.out.println("-------------------------------------------------");
		System.out.println("| Topic:" + topic);
		System.out.println("| Message: " + payload);
		System.out.println("-------------------------------------------------");
		
		// Decodificar el estado
		String estado;
		try {
			estado = new JSONObject(payload).getString("estado");	
		} catch(JSONException e) {
			MySimpleLogger.warn(this.loggerId, "Payload '" + payload + "' no reconocido.");
			return;
		}

		// Do the appropriate action with all of the slaves
		if (estado.equals("ON")) {
			enviarAEsclavos("encender");
		} else if (estado.equals("OFF")) {
			enviarAEsclavos("apagar");
		} else if (estado.equals("BLINK")) {
			enviarAEsclavos("parpadear");
		} else {
			MySimpleLogger.warn(this.loggerId, "Estado no reconocido (solo ON o OFF): " + estado);
		}

	}

	protected void enviarAEsclavos(String accion) {
		for (String esclavo : esclavos) {
			enviarAccion(esclavo, "f1", accion);
		}
		MySimpleLogger.info(this.loggerId, "Sending: " + accion + " to every slave.");
	}

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
			myClient.getTopic(topic).publish(message);
			MySimpleLogger.debug(this.loggerId, topic + " " + pubMsg.toString());
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	protected void subscribe(String myTopic) {
		// subscribe to topic
		try {
			int subQoS = 0;
			myClient.subscribe(myTopic, subQoS);
			MySimpleLogger.info(this.loggerId, "Suscrito al topic " + myTopic);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void connectionLost(Throwable t) {
		MySimpleLogger.debug(this.loggerId, "Connection lost!");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}
}
