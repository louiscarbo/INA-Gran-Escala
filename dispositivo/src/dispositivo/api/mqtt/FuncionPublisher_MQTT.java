package dispositivo.api.mqtt;

import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONObject;

import dispositivo.api.rest.Funcion_Recurso;
import dispositivo.interfaces.Configuracion;
import dispositivo.interfaces.IFuncion;
import dispositivo.utils.MySimpleLogger;

// Ejercicio 9 - Componente que publica el estado de una función en su topic 'info'
public class FuncionPublisher_MQTT {

	protected MqttClient myClient;
	protected MqttConnectOptions connOpt;

	protected String dispositivoId = null;
	protected IFuncion funcion = null;
	protected String mqttBroker = null;

	private String loggerId = null;


	public static FuncionPublisher_MQTT build(String dispositivoId, IFuncion funcion, String mqttBroker) {
		FuncionPublisher_MQTT publisher = new FuncionPublisher_MQTT();
		publisher.dispositivoId = dispositivoId;
		publisher.funcion = funcion;
		publisher.mqttBroker = mqttBroker;
		publisher.loggerId = dispositivoId + "-" + funcion.getId() + "-publisher";
		return publisher;
	}

	protected FuncionPublisher_MQTT() {
	}


	public void connect() {

		// setup MQTT Client
		String clientID = this.dispositivoId + UUID.randomUUID().toString() + ".publisher";
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


	public void publish_status() {

		if ( this.myClient == null || !this.myClient.isConnected() ) {
			this.connect();
		}

		MqttTopic topic = myClient.getTopic(this.calculateInfoTopic());

		// M3 = mismo mensaje de estado que en el API REST: {"id":"f1", "estado":"ON"}
		JSONObject pubMsg = Funcion_Recurso.serialize(this.funcion);

		int pubQoS = 0;
		MqttMessage message = new MqttMessage(pubMsg.toString().getBytes());
		message.setQos(pubQoS);
		message.setRetained(false);

		// Publish the message
		MySimpleLogger.debug(this.loggerId, "Publicando en topic \"" + topic + "\" qos " + pubQoS);
		try {
			topic.publish(message);
			MySimpleLogger.debug(this.loggerId, pubMsg.toString());
		} catch (MqttException e) {
			e.printStackTrace();
		}

	}


	protected String calculateInfoTopic() {
		return Configuracion.TOPIC_BASE + "dispositivo/" + this.dispositivoId + "/funcion/" + this.funcion.getId() + "/info";
	}

}
