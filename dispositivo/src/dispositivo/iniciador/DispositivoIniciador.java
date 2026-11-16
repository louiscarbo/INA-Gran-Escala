package dispositivo.iniciador;

import dispositivo.componentes.Dispositivo;
import dispositivo.componentes.Funcion;
import dispositivo.interfaces.FuncionStatus;
import dispositivo.interfaces.IDispositivo;
import dispositivo.interfaces.IFuncion;

public class DispositivoIniciador {

	public static void main(String[] args) {
		
		if ( args.length < 4 ) {
			System.out.println("Usage: java -jar dispositivo.jar device deviceIP rest-port mqttBroker [mqttUser mqttPass]");
			System.out.println("Example: java -jar dispositivo.jar ttmi050 ttmi050.iot.upv.es 8182 tcp://ttmi008.iot.upv.es:1883");
			return;
		}

		String deviceId = args[0];
		String deviceIP = args[1];
		String port = args[2];
		String mqttBroker = args[3];
		String mqttUser = args.length > 4 ? args[4] : null;
		String mqttPass = args.length > 5 ? args[5] : null;

		
		IDispositivo d = Dispositivo.build(deviceId, deviceIP, Integer.valueOf(port), mqttBroker);
		if ( mqttUser != null ) {
//			d.setBrokerCredentials(mqttUser, mqttPass);
		}
		
		// Añadimos funciones al dispositivo
		IFuncion f1 = Funcion.build("f1", FuncionStatus.OFF);
		d.addFuncion(f1);
		
		IFuncion f2 = Funcion.build("f2", FuncionStatus.OFF);
		d.addFuncion(f2);

		IFuncion f3 = Funcion.build("f3", FuncionStatus.BLINK);
		d.addFuncion(f3);

		// Arrancamos el dispositivo
		d.iniciar();
}

}
