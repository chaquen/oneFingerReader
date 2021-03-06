package com.digitalpersona.onetouch.ui.swing.sample.Enrollment;

import com.digitalpersona.onetouch.*;
import com.digitalpersona.onetouch.verification.*;
import com.mysql.jdbc.exceptions.MySQLDataException;
import java.awt.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class VerificationForm extends CaptureForm 
{
	//Esta variable tambien captura una huella del lector y crea sus caracteristcas para auntetificarla
        // o verificarla con alguna guardada en la BD
        private DPFPVerification Verificador = DPFPGlobal.getVerificationFactory().createVerification();
                
        private DPFPTemplate template;
        
        public DPFPFeatureSet featuresverificacion;
        
        public static String TEMPLATE_PROPERTY = "template";
        Frame mi_frame;
        
	VerificationForm(Frame owner) {
         
          super(owner);
          
        }
    
	@Override protected void init()
	{
		super.init();
		this.setTitle("Verifica tu huella");
		updateStatus(0);
                this.mi_frame=(Frame)super.getOwner();
        }

	
        @Override protected void process(DPFPSample sample) {
            try {
                super.process(sample);
                
                Connection cc=con.conectar();
                //Obtiene la plantilla correspondiente a la persona indicada
                PreparedStatement verificarStmt2 = cc.prepareStatement("SELECT id FROM eventos WHERE estado_evento = 'activo' ");     
                ResultSet rs2 = verificarStmt2.executeQuery();
                int id_e=0;
                while(rs2.next()){
                    id_e= rs2.getInt("id");
                    
                }
                DPFPFeatureSet features = extractFeatures(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);

                Connection c=con.conectar();
                //Obtiene la plantilla correspondiente a la persona indicada
                PreparedStatement verificarStmt = c.prepareStatement("SELECT documento,CONCAT(pri_nombre,' ',pri_apellido) as nombre,updated_at,huella_binaria FROM participantes");     
                ResultSet rs = verificarStmt.executeQuery();
                
                    int i=0;
                    long id_u=0;
                    Boolean no_existe=false;
                while(rs.next()){
                        byte templateBuffer[] = rs.getBytes("huella_binaria");
                        DPFPTemplate referenceTemplate = DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
                        //Envia la plantilla creada al objeto contendor de Template del componente de huella digital
                        setTemplate(referenceTemplate);

                        // Compara las caracteriticas de la huella recientemente capturda con la
                        // alguna plantilla guardada en la base de datos que coincide con ese tipo
                        DPFPVerificationResult result = Verificador.verify(features, getTemplate());

                        //compara las plantilas (actual vs bd)
                        //Si encuentra correspondencia dibuja el mapa
                        //e indica el nombre de la persona que coincidió.
                        if (result.isVerified()){
                            String nombre=rs.getString("nombre");
                            id_u=rs.getLong("documento");                           
                            
                            //crea la imagen de los datos guardado de las huellas guardadas en la base de datos
                            actualizarHuella(id_u,id_e,nombre);
                            
                            return;
                        }else{
                            no_existe=true;
                        }                       
                      
                }
                if(no_existe){
                    JOptionPane.showMessageDialog(null, "Este usuario no aparece registrado, por favor vaya a la opcion registrar huella","Verificacion de Huella", JOptionPane.INFORMATION_MESSAGE);
                }
                //fin while                
            } catch (SQLException ex) {
                Logger.getLogger(VerificationForm.class.getName()).log(Level.SEVERE, null, ex);
                FileManager fl= new FileManager();
                fl.Escribir("SQLException: "+ ex.getMessage());
                JOptionPane.showMessageDialog(null,"Se ha generado un error por favor intenta la verificación de nuevo, si la falla persiste comunicate con el administrador de el sistema","Registro de Huella", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                Logger.getLogger(VerificationForm.class.getName()).log(Level.SEVERE, null, ex);
                FileManager fl= new FileManager();
                fl.Escribir("IOException: "+ ex.getMessage());
                JOptionPane.showMessageDialog(null,"Se ha generado un error por favor intenta la verificación de nuevo, si la falla persiste comunicate con el administrador de el sistema","Registro de Huella", JOptionPane.INFORMATION_MESSAGE);
            }
		
	}
	
  private void updateStatus(int FAR){
        // Show "False accept rate" value
	setStatus(String.format("(FAR) = %1$s", FAR));
  }
  ConexionBD con=new ConexionBD(); 
 
  public DPFPTemplate getTemplate() {
        return template;
  }
  public void setTemplate(DPFPTemplate template) {
        DPFPTemplate old = this.template;
	this.template = template;
	firePropertyChange(TEMPLATE_PROPERTY, old, template);
  }  
  public void actualizarHuella(long id_u,int id_e,String nombre) throws IOException{
     //Obtiene los datos del template de la huella actual
    //Pregunta el nombre de la persona a la cual corresponde dicha huella
     try {
            
            if(id_e==0){
                JOptionPane.showMessageDialog(null, "Bienvenido, "+nombre+" recuerda que para registarte debes seleccionar un evento","Verificacion de Huella", JOptionPane.INFORMATION_MESSAGE);
                
            }else{
                Connection c=con.conectar(); //establece la conexion con la BD
                PreparedStatement guardarStmt = c.prepareStatement("UPDATE participantes SET estado_registro = ? WHERE documento = ? ");
                guardarStmt.setString(1, "verificado");
                //guardarStmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                guardarStmt.setLong(2, id_u);
                //Ejecuta la sentencia
                guardarStmt.execute();
                guardarStmt.close();
                //JOptionPane.showMessageDialog(null,"Huella Guardada Correctamente");
                con.desconectar();
                Connection c2=con.conectar();
                //Obtiene la plantilla correspondiente a la persona indicada
                PreparedStatement verificarStmt2 = c2.prepareStatement("SELECT id FROM detalle_participantes WHERE user_id = ? AND event_id = ?  ");     
                verificarStmt2.setLong(1, id_u);
                verificarStmt2.setInt(2, id_e);
                ResultSet rs2 = verificarStmt2.executeQuery();
                int id_ex=0;
                while(rs2.next()){
                    id_ex= rs2.getInt("id");
                    
                }                
                con.desconectar();
                if(id_ex==0){
                       
                        
                        validaTerminos(id_u,id_e,nombre);                     
                        
                }else{
                        JOptionPane.showMessageDialog(null, nombre+", ya te habias registrado a este evento","Verificacion de Huella", JOptionPane.INFORMATION_MESSAGE);
                        //temporal solo para probar 
                                         
                }
            }     
     }catch(MySQLDataException exm){
         System.err.println("Error al guardar dato los datos de la huella."+ exm.getMessage());
         FileManager fl= new FileManager();
         fl.Escribir("MySQLDataException: "+ exm.getMessage());   
         JOptionPane.showMessageDialog(null,"Se ha generado un error por favor intenta la verificación de nuevo, si la falla persiste comunicate con el administrador de el sistema","Registro de Huella", JOptionPane.INFORMATION_MESSAGE);
     }
     catch (SQLException ex) {
     //Si ocurre un error lo indica en la consola
        System.err.println("Error al guardar los datos de la huella."+ ex.getMessage());
        FileManager fl= new FileManager();
        fl.Escribir("SQLException: "+ ex.getMessage());  
        JOptionPane.showMessageDialog(null,"Se ha generado un error por favor intenta la verificación de nuevo, si la falla persiste comunicate con el administrador de el sistema","Registro de Huella", JOptionPane.INFORMATION_MESSAGE);
     }finally{
        con.desconectar();
     }
   }
   private void validaTerminos(long user,int event,String nombre) {
		ValidarTerminos form = new ValidarTerminos(this.mi_frame,user,event,nombre);
                form.setVisible(true);
                
   }
}
