package it.unibs.ricoperativa;

import java.io.*;
import gurobi.*;
import gurobi.GRB.*;

public class Main {

	private static int m, n, mat[][], r_i[], s_j[], k, alfa_j[], h, x, y;
	private static double c, valObj;
	
	public static void main(String[] args) throws Exception{
		readFile();

		
		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);
		GRBVar[][] xij = aggiungiVariabili(model, s_j, r_i);
		aggiungiFunzioneObiettivo(model, xij, mat);
		aggiungiVincoliMagazzino(model, xij, s_j);
		aggiungiVincoliDomanda(model, xij, r_i);
		
		risolvi(model);
		//Stampa valore della funzione obbiettivo
		System.out.println(model.get(GRB.DoubleAttr.ObjVal));
		stampaSoluzione(model, xij);

	}
	
	/*
	 * Metodo per stampare i risultati del problema
	 */
	private static void stampaSoluzione(GRBModel model, GRBVar[][] xij) throws Exception {
		System.out.println("GRUPPO 11");
		System.out.println("Componenti: Bagnalasta Federico, Kovacic Matteo");
		
		System.out.println("\nQUESITO I:");
		//System.out.printf("funzione obbiettivo = %.4f\n" + model.get(GRB.DoubleAttr.ObjVal));                 //DA CORREGGERE %.4f 
		/*
		for(GRBVar var : model.getVars()) {
			System.out.println(var.get(StringAttr.VarName) + ": "+ var.get(DoubleAttr.X));                      //DA TENERE
		}
		/*
		int variabiliNonAzzerate = 0;
		int ccrAzzerati = 0;
		
			
			if((var.get(DoubleAttr.X) != 0)) {
				variabiliNonAzzerate++;
			}
			
			if((var.get(DoubleAttr.RC)) == 0) {
				ccrAzzerati++;
			}
		}
	//==========================================================================
		
		//PROVE
		//DA CANCELLARE
		
		for(GRBVar var : model.getVars()) {
			
			//if(var.get(DoubleAttr.RC) == 0)
			System.out.println(var.get(StringAttr.VarName) + ": "+ var.get(DoubleAttr.RC));
		}
		System.out.println(variabiliNonAzzerate);
		System.out.println(ccrAzzerati);    //DOVREBBERO ESSERE ALMENO 86
		
		//==========================================================================
		variabiliBase = m + n;
		if(variabiliNonAzzerate == variabiliBase) {
			System.out.println("Degenere: no");
		}
		//Soluzione degenere se ci sono delle variabili in base con valore nullo
		else {
			System.out.println("Degenere: sì");
		}
		if(ccrAzzerati == variabiliBase) {
			System.out.println("Multipla: no");
		}
		//Soluzione multipla se almeno una soluzione fuori base ha ccr nullo
		else {
			System.out.println("Multipla: sì");
		}
		
		*/
		
		controllaDegenere(model);
		controllaMultipla(model);
		
		
		System.out.println("\nQUESITO II:");
	
		
		trovaVincoliInattivi(model);
		
		solDuale(model);
		
		trovaIntervalloK(model, xij);
		//Forse bisogna aggiornare xij, con aggiungiVariabili
		
		
		
		//trovaRilassamento(model);
	}
	
	/*
	 * Metodo per torvare l'intervallo del parametro k, nel quale il problema non ha soluzione
	 */
	/*
	 * Dopo la risoluzione del modello, viene verificato lo stato di fattibilità del problema. 
	 * Se il problema è infattibile, viene impostato un intervallo iniziale [0, raggioMax] 
	 * e viene utilizzato il metodo della bisezione per ridurre l'intervallo fino a che non si individua il valore massimo di raggio per cui il problema è ancora infattibile. 
	 * Questa operazione viene eseguita all'interno del ciclo while. 
	 *Nel ciclo, viene calcolato il valore del raggio medio e viene impostato un vincolo quadratico per ogni coppia di nodi (i,j) che limita la distanza tra i nodi a essere minore o uguale al quadrato del raggio medio. 
	 *Viene poi risolto il modello e se il problema rimane infattibile, il valore massimo del raggio viene impostato a raggio medio. 
	 *Altrimenti, il valore minimo del raggio viene impostato a raggio medio. 
	 *Infine, quando l'intervallo viene sufficientemente ridotto, viene stampato il valore massimo del raggio per cui il problema diventa infattibile. 
	 *Se invece il problema era già fattibile, viene stampato il messaggio "Il problema ammette soluzione per ogni valore di raggio <= raggioMax".
	 */
	private static void trovaIntervalloK(GRBModel model, GRBVar[][] xij) throws GRBException {
		double raggioMin = 0.0, raggioMax = k;
		 if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			 while((raggioMax - raggioMin) > 1) {									//ARRIVA SEMPRE ALLA FINE
				 double raggio = (raggioMax - raggioMin)/2;
				 	for(int i = 0; i < m; i++) {
				 		for(int j = 0; j < n; j++) {																//NON ESISTE UN CONTROLLO SE K E' SEMPRE VALIDO!
				 			if(mat[i][j] > raggio) {
				 				xij[i][j].set(GRB.DoubleAttr.UB, 0.0);
				 				/*GRBVar v = xij.getVars()[0];
				 						  v.set(GRB.DoubleAttr.UB, 0);
				 				/*
				 				GRBVar v = xij[i][j].get(GRB.DoubleAttr.X);
				 				v.set(GRB.DoubleAttr.UB, 0.0);
				 				*/
							}
				 			// minVar.set(GRB.DoubleAttr.UB, 0.0);
							
				 		}
				 	}
				 	model.optimize();
				 	//Se non esiste una soluzione, la variabile min assumerà valore = al raggio 
				 	 if (model.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
				          raggioMin = raggio;																			//PERCHE' MAX?
				     } else {
				          raggioMax = raggio;
				     }
			 }
			 System.out.println(String.format("intervallo k = (%f, %f)", 0.0,  raggioMax));
		 }
	}
	
	/*
	 * Metodo per trovare la soluzione del problema duale 
	 */
	private static void solDuale(GRBModel model) throws Exception {
		int i = 0;
		for(GRBConstr constr : model.getConstrs()){
		//for(int i = 0; i < model.getConstrs().length; i++)	
			System.out.printf(String.format("lambda_%s %.4f\t", i++, constr.get(DoubleAttr.Pi)));                       //IL VALORE DOVREBBE ESSERE POSITIVO
		}
	}
	
	/*
	 * Metodo per trovare il rilassamento
	 */
	private static void trovaRilassamento(GRBModel model) throws Exception {
		GRBModel rilassamento = model.relax();                                                                           //STAMPARE IL VALORE
		rilassamento.optimize();
	}
	
	/*
	 * Metodo per verificare se la soluzione è degenere
	 */
	private static void controllaDegenere(GRBModel model) throws Exception {
		if (model.get(GRB.IntAttr.SolCount) > 0) {
            double[] solution = model.get(GRB.DoubleAttr.X, model.getVars());

            //Verifica se esistono variabili di base con valore 0
            boolean isDegenerate = false;
            for (GRBVar var : model.getVars()) {
                if (var.get(GRB.IntAttr.VBasis) == GRB.BASIC && solution[var.get(GRB.IntAttr.VBasis)] == 0.0) {
                    isDegenerate = true;
                    break;
                }
            }
            if (isDegenerate) {
            	 System.out.println("Degenere: sì");
            }
            else {
            	System.out.println("Degenere: no");
            }
            
        } else {
            System.out.println("Il problema non ha soluzione");
        }
	}
	
	/*
	 * Metodo per verificare se la soluzione è multipla
	 */
	private static void controllaMultipla(GRBModel model) throws Exception {
		if(model.get(GRB.IntAttr.SolCount) > 1)
			System.out.println("Multipla: sì");
		else
			System.out.println("Multipla: no");
	}
	
	/*
	 * Metodo per individuare i vincoli inattivi
	 */
	private static void trovaVincoliInattivi(GRBModel model) throws GRBException {
		//Un vincolo è inattivo se l'attributo Slack è > 0
		System.out.print("lista vincoli non attivi = ");
		GRBConstr[] constrains = model.getConstrs();
		for(int i = 0; i < constrains.length; i++) {
			double slack = constrains[i].get(GRB.DoubleAttr.Slack);
			if(slack > 0.0) {
				System.out.printf("  " + constrains[i].get(GRB.StringAttr.ConstrName));
			}
		}
		System.out.println("");
	}

	/*
	 * Metodo per l'aggiunta delle variabili
	 */
	private static GRBVar[][] aggiungiVariabili(GRBModel model, int[] magazzino, int[] domanda) throws GRBException {
		GRBVar[][] xij = new GRBVar[magazzino.length][domanda.length];
		
		for(int i = 0; i < magazzino.length; i++) {
			for(int j = 0; j < domanda.length; j++) {
				double upperBound = GRB.INFINITY;
				if(mat[i][j] > k) {
					upperBound = 0;
				}
				xij[i][j] = model.addVar(0, upperBound, 0, GRB.CONTINUOUS, "xij_" + i + "_" + j);
			}
		}
		return xij;
	}
	
	/*
	 * Metodo per l'aggiunta della funzione obiettivo
	 */
	private static void aggiungiFunzioneObiettivo(GRBModel model, GRBVar[][] xij, int[][] distanze) throws GRBException {
		GRBLinExpr obj = new GRBLinExpr();
		
		for(int i = 0; i < distanze.length; i++) {
			for(int j = 0; j < distanze[0].length; j++) {
				//MOLTIPLICARE LA DISTANZA PER IL COSTO UNA VOLTA SISTEMATE LE CIFRE DECIMALI ALTRIMENTI VA TUTTO A 0
				obj.addTerm(c * (double) (distanze[i][j]), xij[i][j]);
			}
		}
		model.setObjective(obj);
		model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
	}
	
	/*
	 * Metodo per l'aggiunta dei vincoli di magazzino
	 */
	private static void aggiungiVincoliMagazzino(GRBModel model, GRBVar[][] xij, int[] magazzino) throws GRBException {
		for(int i = 0; i < magazzino.length; i++) {
			GRBLinExpr expr = new GRBLinExpr();
			
			for(int j = 0; j < xij[0].length; j++) {
				expr.addTerm(1, xij[i][j]);
			}
			model.addConstr(expr, GRB.LESS_EQUAL, magazzino[i], "vincolo_produzione_i_"+i);
		}
	}
	
	/*
	 * Metodo per l'aggiunta dei vincoli di domanda
	 */
	private static void aggiungiVincoliDomanda(GRBModel model, GRBVar[][] xij, int[] domanda) throws GRBException {
		for(int j = 0; j < domanda.length; j++) {
			GRBLinExpr expr = new GRBLinExpr();
			
			for(int i = 0; i < xij.length; i++) {
				expr.addTerm(1, xij[i][j]);
			}
			model.addConstr(expr, GRB.GREATER_EQUAL, domanda[j], "vincolo_domanda_j_"+j);
		}
	}
	
	/*
	 * Metodo per risolvere il problema
	 */
	private static void risolvi(GRBModel model) throws GRBException {
		model.optimize();
		
		int status = model.get(GRB.IntAttr.Status);
		
		System.out.println("\n\n\nStato Ottimizzazione: "+ status); 
		// 2 soluzione ottima trovata
		// 3 non esiste soluzione ammissibile (infeasible)
		// 5 soluzione illimitata
		// 9 tempo limite raggiunto
		
		/*for(GRBVar var : model.getVars()) {
			System.out.println(var.get(StringAttr.VarName)+ ": "+ var.get(DoubleAttr.X));
		}*/
	}
	
	/**
	 * Metodo per impostare i parametri del problema
	 * @param env
	 * @throws GRBException
	 */
	private static void impostaParametri(GRBEnv env) throws GRBException 
	{
		//Metodo del simplesso duale
		env.set(GRB.IntParam.Method, 0);
		env.set(GRB.IntParam.Presolve, 0);
		env.set(GRB.DoubleParam.TimeLimit, 600);

	}
	
	/*
	 * Metodo per la lettura del file che contiene i dati del problema
	 */
	private static void readFile() throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader("coppia_11.txt"));
		
		String[] stArray;
		String st = reader.readLine();
		
		while(st!= null) {
			stArray = st.split(" ");
			
			//Switch con cui viene determinata la variabile dove salvare i dati del file
			switch (stArray[0]) {
			
				//Lettura numero di magazzini (RIGHE)
				case "m":
					m = Integer.parseInt(stArray[1]);
					System.out.println(m);
					break;
					
				//Lettura numero di clienti(COLONNE)
				case "n":
					n = Integer.parseInt(stArray[1]);
					System.out.println(n);
					break;
					
				//Lettura matrice
				case "matrice":
					mat = new int[m][n];
					for(int i = 0; i < m; i++) {
						st = reader.readLine();
						stArray = st.split(" ");
						for(int l = 0; l < n; l++) {
							mat[i][l] = Integer.parseInt(stArray[l]);
							System.out.print(mat[i][l]);
						}
					}

					break;
					
				//Lettura richieste dei clienti	
				case "r_i":
					r_i = new int[n];
					System.out.println();
					st = reader.readLine();
					stArray = st.split(" ");
					for(int i = 0; i < n; i++) {
						r_i[i] = Integer.parseInt(stArray[i]);
						System.out.print(r_i[i]);
					}
					break;
					
				//Lettura della capacità dei magazzini 	
				case "s_j":
					s_j = new int[m];
					System.out.println();
					st = reader.readLine();
					stArray = st.split(" ");
					for(int i = 0; i < m; i++) {	
						s_j[i] = Integer.parseInt(stArray[i]);
						System.out.print(s_j[i]);
					}
					break;	
					
				//Lettura costo in euro per trasportare 1kg di merce per 1km
				case "c":
					c = Double.parseDouble(stArray[1]);
					System.out.println("\n" + c);
					break;
					
				//Lettura raggio massima distanza
				case "k": 
					k = Integer.parseInt(stArray[1]);
					System.out.print(k);
					break;
					
				//Lettura dei dati relativi alla chiusura di un magazzino
				case "alfa_j":
					alfa_j = new int[m];
					System.out.println();
					st = reader.readLine();
					stArray = st.split(" ");
					for(int i = 0; i < m; i++) {	
						alfa_j[i] = Integer.parseInt(stArray[i]);
						System.out.print(alfa_j[i]);
					}
					break;
				
				//Lettura pedice del parametro s
				case "h":
					h = Integer.parseInt(stArray[1]);
					System.out.println();
					System.out.println(h);
					break;
				
				//Lettura pedici del parametro d
				case "x":
					x = Integer.parseInt(stArray[2]);
					y = Integer.parseInt(stArray[3]);
					System.out.println(x);
					System.out.println(y);
					break;
			}
			//Lettura della linea successiva
			st = reader.readLine();
		}
		reader.close();
	}
}