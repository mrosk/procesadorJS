package sintactico;

//Paquetes internos
import lexico.*;
import error.*;
import tabla_simbolos.*;
import token.*;

//Paquetes externos
import java.io.*;

public class Sintactico {

  //Atributos de clase
  private Lexico analizador;
  private TablaSimbolos tS;
  private Token tokenDevuelto;
  private Token tokenLlamador;
  private static Token nombreFuncion;
  public static File miDir = new File(".");

  //Atributos para escritura de fichero
  private BufferedWriter tablasWriter;
  private BufferedWriter parseWriter;
  private BufferedWriter errorWriter;

  //Atributos basicos
  private String parse = null;
  private String tipo = null;
  private int ancho = 0;
  private int tabla = 0;
  private boolean declaracion;
  public static boolean flagDeclaracionLocal = false;
  public static boolean flagDeclaracion = false;
  private static boolean flagReturn = true;

  //Constructor: inicializar los archivos y atributos necesarios.
  public Sintactico() {

    //Inicializando atributos de clase
    this.analizador = new Lexico();
    this.tS = new TablaSimbolos();
    this.tokenDevuelto = new Token(null, null);
    this.tokenLlamador = new Token(null, null);
    this.nombreFuncion = new Token(null, null);

    //Inicializando los atributos basicos
    this.parse = "";
    this.tabla = 0;

    //Nuevos archivos
    File archivoTablas = new File(miDir + "\\tablas.txt");
    File archivoParse = new File(miDir + "\\parse.txt");
    File archivoError = new File(miDir + "\\error.txt");

    try {
      this.tablasWriter = new BufferedWriter(new FileWriter(archivoTablas));
      this.parseWriter = new BufferedWriter(new FileWriter(archivoParse));
      this.errorWriter = new BufferedWriter(new FileWriter(archivoError));
    } catch (IOException e) {
      System.out.println("Error: inicializacion de los ficheros tablas, parse y error.");
    }

  }

  //Empareja:
  public void empareja(Token valor) {
    
    if (valor != null && valor.equals(tokenDevuelto)) {
      tokenDevuelto = analizador.al(tS);
    }
    else {
      throw new Error("Error en la linea: "+Integer.toString(Lexico.linea));
    }

  } 

  //ProcedP: 
  //First de P: var if id prompt write function eof
  public void procedP() {

    //P -> B Z P
    if ("VAR".equals(this.getTokenDevuelto().getId()) || "IF".equals(this.getTokenDevuelto().getId()) || "ID".equals(this.getTokenDevuelto().getId()) || "PROMPT".equals(this.getTokenDevuelto().getId()) || "WRITE".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "1 ");
      
      procedB();
      procedZ();
      procedP();
    }
    //P -> Fq Z P
    else if ("FUNCTION".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "2 ");
      
      procedFq();
      procedZ();
      procedP();
    }
    //P -> eof
    else if ("EOF".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "3 ");
    }
    else {
      System.out.println("Error en procedP");
    }

  }

  //ProcedB:
  //First de B: var if id prompt write
  public void procedB() {

    //B -> var F id D D1
    if ("VAR".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "4 ");
      
      flagDeclaracion = true;
      empareja(new Token("VAR", null));
      flagDeclaracion = false;

      procedF();
      empareja(new Token("ID", null));
      procedD();
      procedD1();
    }
    //B -> if ( E ) G
    else if ("IF".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "5 ");

      empareja(new Token("IF", null));
      empareja(new Token("PARARENTABIERTO", null));
      procedE();
      empareja(new Token("PARARENTCERRADO", null));
      procedG();
    }
    //B -> S
    else if ("WRITE".equals(this.getTokenDevuelto().getId()) || "PROMPT".equals(this.getTokenDevuelto().getId()) || "ID".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "6 ");

      procedS();
    }
    else {
      System.out.println("Error en procedB");
    }
  }
  //ProcedS:
  //First de S: id prompt write
  public void procedS() {

    //S -> id S1
    if ("ID".equals(this.getTokenDevuelto().getId())) {
          this.setParse(this.getParse() + "7 ");

          tokenLlamador = tokenDevuelto;
          empareja(new Token("ID", null));
          procedS1();
      }
      //S -> prompt ( id )
      else if ("PROMPT".equals(this.getTokenDevuelto().getId())) {
          this.setParse(this.getParse() + "8 ");
          
          empareja(new Token("PROMPT", null));
          empareja(new Token("PARENTABIERTO", null));

          //Varible no declarada: en este caso es global y entera.
          if (tS.getTipo(tokenDevuelto) == null) {
            tS.addTipo(tokenDevuelto, "ENTERA");
            tS.addDireccion(tokenDevuelto, 2);
          }

          empareja(new Token("ID", null));
          empareja(new Token("PARENTCERRADO", null));
      }
      //S -> write ( E )
      else if ("WRITE".equals(this.getTokenDevuelto().getId())) {
          this.setParse(this.getParse() + "9 ");
          
          empareja(new Token("WRITE", null));
          empareja(new Token("PARENTABIERTO", null));
          procedE();
          empareja(new Token("PARENTCERRADO", null));
      }
      else {
        System.out.println("Error en procedS");
      }

  }

  //ProcedS1:
  //First de S1: ( = /
  public void procedS1() {

    //S1 -> = E ;
    if ("IGUAL".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "10 ");

      empareja(new Token("IGUAL", null));
      procedE();
      if ("VOID".equals(tipo)) {
        System.out.println("Error de asignacion en el metodo de procedS1");
      }

      tS.addTipo(tokenLlamador, tipo);
      tS.addDireccion(tokenLlamador, ancho);
      empareja(new Token("PUNTOYCOMA",  null));
    }
    //S1 -> ( L ) ;
    else if ("PARENTABIERTO".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "11 ");

      int contParam = 0;
      
      empareja(new Token("PARENTABIERTO", null));

      contParam = procedL();
      if (nombreFuncion != null && nombreFuncion.equals(tokenLlamador) && tS.getNParametrosGlobal(tokenLlamador) == contParam) {
              tS.buscaTSGlobal(tokenLlamador.getValor());
      }
      else if (tS.buscaTS(tokenLlamador.getValor())[0] == null || !"FUNC".equals(tS.getTipo(tokenLlamador))) {//tipo != FUNC porque si es variable o no declarada tiene que dar error.
          System.out.println("La funcion no ha sido declarada en el metodo procedS1");
      }
      else if (tS.getNParametros(tokenLlamador) != contParam) {
          System.out.println("Error en los parametros de la funcion que ha sido llamada");
      }
      empareja(new Token("PARENTCERRADO", null));
      empareja(new Token("PUNTOYCOMA", null));
    }
    //S1 -> /= E ;
    else if ("ASIGDIV".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "12 ");

      empareja(new Token("ASIGDIV", null));
      procedE();
      if ("VOID".equals(tipo)) {
        System.out.println("Error de asignacion en el metodo de procedS1");
      }
      this.tS.addTipo(tokenLlamador, tipo);
      this.tS.addDireccion(tokenLlamador, ancho);
    }
    else {
      System.out.println("Error en procedS1");
    }
  }

  //ProcedFq:
  //First de Fq: function
  public void procedFq() {

    int contParam = 0;

    //Fq -> function F id ( A ) Z { Z Cfun }
    if ("FUNCTION".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "13 ");

      flagDeclaracion = true;
      empareja(new Token("FUNCTION", null));
      nombreFuncion = this.getTokenDevuelto();
      flagDeclaracion = false;

      //Ahora creamos la tabla de simbolos local: suponemos que un puntero ocupa 4 bytes.
      this.tS.addTipo(tokenDevuelto, "FUNCTION");
      this.tS.addDireccion(tokenDevuelto, 4);
      this.tS.crearTSL();

      TablaSimbolos tLocal = (TablaSimbolos) this.tS.getTablaSimbolos().get(this.tS.getContadorRegistro() - 1)[0];
      if (tLocal != null) {
        //Palabras reservadas a la tabla local: REVISAR
        tLocal.vaciarTabla();
      }

      procedF();
      empareja(new Token("ID", null));

      flagDeclaracion = true;
      empareja(new Token("PARENTABIERTO", null));
      contParam = procedA();
      //Almacenamos el numero de parametros
      this.tS.addParametros(contParam);
      flagDeclaracion = false;

      empareja(new Token("PARENTCERRADO", null));
      procedZ();
      empareja(new Token("LLAVEABIERTA", null));
      procedZ();
      procedCfun();

      //Escribimos y posteriormente borramos la tabla de simbolos local.
      this.tS.addEtiqueta();
      this.tablasWriter.write("TABLA LOCAL DE LA FUNCION: "+nombreFuncion.getValor());
      this.tS.volcarTabla(tablasWriter);
      this.tS.borrarTS();

      empareja(new Token("LLAVECERRADA", null));

      if (flagReturn) {
        this.tipo = "VOID";
      }
      this.tS.addDevuelve(this tipo);
      nombreFuncion = null;
    }
    else {
      System.out.println("Error en procedFq");
    }

  }

  //ProcedZ: debemos detectar el salto de linea.
  //First de Z: cr
  public void procedZ() {

    //Z -> cr Z1
    if ("CR".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse + "14 ");

      empareja(new Token("CR", null));
      procedZ1();
    }
    else {
      System.out.println("Error en procedZ");
    }

  }

  //ProcedZ1: debemos detectar el salto de linea.
  //First de Z1: cr lambda
  public void procedZ1() {

    //Z1 -> Z
    if ("CR".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "15 ");

      procedZ();
    }
    //Z1 -> lambda
    else {
      this.setParse(this.getParse() + "16 ");
    }

  }

  //ProcedA:
  //First de A: int bool chars lambda
  public int procedA() {

    int contParam = 0;

    //A -> F id A1
    if ("NUM".equals(this.getTokenDevuelto().getId()) || "CHARS".equals(this.getTokenDevuelto().getId()) || "BOOL".equals(this.getTokenDevuelto().getId())) {
      this.setParse(this.getParse() + "17 ");

      //Incluimos en la tabla de simbolos la variable.
      procedF();
      empareja(new Token("ID", null));
      contParam = procedA1();
      contParam++;
    }
    //A -> lambda
    else {
      this.setParse(this.getParse() + "18 "); 
    }

    return contParam;

  }

  //ProcedA1:
  //First de A1: , lambda
  public int procedA1() {

    int contParam = 0;
    //A1 -> , A
    if ("COMA".equals()this.getTokenDevuelto().getId()) {
      this.setParse(this.getParse() + "19 ");

      empareja(new Token("COMA", null));
      procedA();
    }
    //A1 -> lambda
    else {
      this.setParse(this.getParse() + "20 ");
    }

  }

}