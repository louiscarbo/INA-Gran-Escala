package dispositivo.interfaces;

public interface IFuncion {
	
	public String getId();
	
	public IFuncion iniciar();
	public IFuncion detener();
	
	public IFuncion encender();
	public IFuncion apagar();
	public IFuncion parpadear();
	
	public FuncionStatus getStatus();

	public boolean estaHabilitada();
	public void habilitar();
	public void deshabilitar();
}
