package it.unibs.ricoperativa;

import java.io.*;
import java.util.ArrayList;

import gurobi.*;
import gurobi.GRB.DoubleAttr;
import gurobi.GRB.StringAttr;

public class Main {

private static int m, n, mat[][], r_i[], s_j[], k, alfa_j[], h, x, y;
private static double c;

	public static void main(String[] args) throws Exception {				
						
		readFile();
		
		GRBEnv env = new GRBEnv();
		impostaParametri(env);
		
		GRBModel model = new GRBModel(env);
		model.set(GRB.IntParam.LogToConsole, 0);
		GRBVar[][] xij = aggiungiVariabili(model);
		aggiungiFunzioneObiettivo(model, xij);
		aggiungiVincoliMagazzino(model, xij);
		aggiungiVincoliDomanda(model, xij);
		
		risolvi(model);
		
		stampaSoluzione(model, env);
	}
	
	//Metodo per impostare i parametri dell'environment
	private static void impostaParametri(GRBEnv env) throws GRBException {
		env.set(GRB.IntParam.Method, 0);
	}
	
	//Metodo per stampare la soluzione
	private static void stampaSoluzione(GRBModel model, GRBEnv env) throws GRBException {
		System.out.println("\n\nGRUPPO <11>");
		System.out.println("Componenti: <Bagnalasta Federico, Kovacic Matteo>");
		
		//=================================================================
		
		System.out.println("\nQUESITO I:");
		
		System.out.printf("funzione obiettivo = <%.4f>\n", model.get(GRB.DoubleAttr.ObjVal));
		
		stampaVariabili(model);
		
		controllaDegenere(model);
		controllaMultipla(model);
		
		//=================================================================
		
		System.out.println("\nQUESITO II:");
		
		trovaVincoliInattivi(model);
		
		trovaIntervalloK(env);
		
		ottimoDuale(model);
		
		sensitivitaMagazzino(model);
		
		sensitivitaDistanza(model);
		
		
		//=================================================================
		
		System.out.println("\nQUESITO III:");
		
		creaModulo3(env);
	}
	
	//Metodo per aggiungere le variabili
	private static GRBVar[][] aggiungiVariabili(GRBModel model) throws GRBException {
		GRBVar[][] xij = new GRBVar[s_j.length][r_i.length];
	
		for(int i = 0; i < s_j.length; i++) {
			for(int j = 0; j < r_i.length; j++) {
				double upperBound = GRB.INFINITY;
				if(mat[i][j] > k) {
				upperBound = 0;
				}
			xij[i][j] = model.addVar(0, upperBound, 0, GRB.CONTINUOUS, "x_" + i + "_" + j);
			}
		}
		return xij;
	}
	
	//Metodo per aggiungere la funzione obiettivo
	private static void aggiungiFunzioneObiettivo(GRBModel model, GRBVar[][] xij) throws GRBException {
		GRBLinExpr obj = new GRBLinExpr();
		
		for(int i = 0; i < mat.length; i++) {
			for(int j = 0; j < mat[0].length; j++) {
				obj.addTerm(c * mat[i][j], xij[i][j]);
			}
		}
		model.setObjective(obj);
		model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
	}
	
	//Metodo per aggiungere i vincoli di magazzino
	private static void aggiungiVincoliMagazzino(GRBModel model, GRBVar[][] xij) throws GRBException {
		for(int i = 0; i < s_j.length; i++) {
			GRBLinExpr expr = new GRBLinExpr();
		
			for(int j = 0; j < xij[0].length; j++) {
				expr.addTerm(1, xij[i][j]);
			}
			model.addConstr(expr, GRB.LESS_EQUAL, s_j[i], "vincolo_produzione_i_" + i);
		}
	}

	//Metodo per aggiungere vincoli di domanda
	private static void aggiungiVincoliDomanda(GRBModel model, GRBVar[][] xij) throws GRBException {
		for(int j = 0; j < r_i.length; j++) {
			GRBLinExpr expr = new GRBLinExpr();
		
			for(int i = 0; i < xij.length; i++) {
				expr.addTerm(1, xij[i][j]);
			}
			model.addConstr(expr, GRB.GREATER_EQUAL, r_i[j], "vincolo_domanda_j_" + j);
		}
	}
	
	//Metodo per risolvere il problema 
	private static void risolvi(GRBModel model) throws GRBException {
		model.optimize();
	}

	//Metodo per leggere il file che contiene i dati del problema
	private static void readFile() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("coppia_11.txt"));
		
		String[] stArray;
		String st = reader.readLine();
		
		while(st!= null) {
			stArray = st.split(" ");
			
			//Switch con cui viene determinata la variabile in cui salvare i dati del file
			switch (stArray[0]) {
			
			//Lettura numero di magazzini (RIGHE)
			case "m":
				m = Integer.parseInt(stArray[1]);
				break;
			
			//Lettura numero di clienti (COLONNE)
			case "n":
				n = Integer.parseInt(stArray[1]);
				break;
			
			//Lettura matrice
			case "matrice":
				mat = new int[m][n];
				for(int i = 0; i < m; i++) {
					st = reader.readLine();
					stArray = st.split(" ");
					for(int j = 0; j < n; j++) {
						mat[i][j] = Integer.parseInt(stArray[j]);
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
				}
				break;
				
			//Lettura della capacita' dei magazzini 
			case "s_j":
				s_j = new int[m];
				System.out.println();
				st = reader.readLine();
				stArray = st.split(" ");
				for(int i = 0; i < m; i++) { 
					s_j[i] = Integer.parseInt(stArray[i]);
				}
				break; 
			
			//Lettura costo in euro per trasportare 1kg di merce per 1km
			case "c":
				c = Double.parseDouble(stArray[1]);
				break;
			
			//Lettura raggio massima distanza
			case "k": 
				k = Integer.parseInt(stArray[1]);
				break;
			
			//Lettura dei dati relativi alla chiusura di un magazzino
			case "alfa_j":
				alfa_j = new int[m];
				st = reader.readLine();
				stArray = st.split(" ");
				for(int i = 0; i < m; i++) { 
					alfa_j[i] = Integer.parseInt(stArray[i]);
				}
				break;
			
			//Lettura pedice del parametro s
			case "h":
				h = Integer.parseInt(stArray[1]);
				break;
			
			//Lettura pedici del parametro d
			case "x":
				x = Integer.parseInt(stArray[2]);
				y = Integer.parseInt(stArray[3]);
				break;
			}
			//Lettura della linea successiva
			st = reader.readLine();
		}
		reader.close();
	}


//===========================================================================================

// QUESITO I

	//Metodo per stamapare il valore ottimo delle variabili
	private static void stampaVariabili(GRBModel model) throws GRBException {
		for(GRBVar var : model.getVars()) { 
			System.out.printf("<%s> = <%.4f>\n", var.get(StringAttr.VarName), var.get(DoubleAttr.X));
		}
	}
	
	//Metodo per controllare se la soluzione e' degenere
	private static void controllaDegenere(GRBModel model) throws GRBException {
		if(model.get(GRB.IntAttr.SolCount) > 0) {
		            double[] solution = model.get(GRB.DoubleAttr.X, model.getVars());
		
		            //Verifica se esistono variabili di base con valore = 0
		            boolean isDegenerate = false;
		            for(GRBVar var : model.getVars()) {
		                if(var.get(GRB.IntAttr.VBasis) == GRB.BASIC && solution[var.get(GRB.IntAttr.VBasis)] == 0.0) {
		                    isDegenerate = true;
		                    break;
		                }
		            }
		            if(isDegenerate) {
		            System.out.println("Degenere: sì");
		            }
		            else {
		            System.out.println("Degenere: no");
		            }
		            
		        } else {
		            System.out.println("Il problema non ha soluzione");
		        }
	}

	//Metodo per controllare se la soluzione e' multipla
	private static void controllaMultipla(GRBModel model) throws GRBException {
		//Conta quante variabili iniziali fuori base hanno ccr = 0
		int count = 0;
		for(GRBVar var : model.getVars()) {
			if(var.get(GRB.IntAttr.VBasis) != GRB.BASIC && var.get(GRB.DoubleAttr.RC) == 0) {
				count++;
			}
		}
			if(count != 0) {
				System.out.println("Multipla: sì");
			}
			else {
				//Conta quante slack fuori base hanno shadow price = 0
				int count2 = 0;
				for(GRBConstr constr : model.getConstrs()) {
					if(constr.get(GRB.IntAttr.VBasis) != GRB.BASIC && constr.get(DoubleAttr.Pi) == 0) {
						count2++;
					}
				}
				if(count2 != 0) {
					System.out.println("Multipla: sì");
				}
				else {
					System.out.println("Multipla: no");
				}
			}
	}


//===========================================================================================

// QUESITO II

	//Metodo per trovare i vincoli inattivi
	private static void trovaVincoliInattivi(GRBModel model) throws GRBException {
		//Un vincolo e' inattivo se l'attributo Slack e' > 0
		System.out.print("lista vincoli non attivi = [");
		GRBConstr[] constrains = model.getConstrs();
		for(int i = 0; i < constrains.length; i++) {
			double slack = constrains[i].get(GRB.DoubleAttr.Slack);
			if(slack > 0.0) {
				System.out.print("<" + constrains[i].get(GRB.StringAttr.ConstrName) + ">, ");
			}
		}
		System.out.print("]");
	}

	//Metodo per trovare l'intervallo di k per cui il problema non ha soluzione
	private static void trovaIntervalloK(GRBEnv env) throws GRBException {
		//Creazione nuovo problema
		GRBModel model2 = new GRBModel(env);
		model2.set(GRB.IntParam.LogToConsole, 0);
		
		GRBVar[][] xij2 = aggiungiVariabili(model2);
		aggiungiFunzioneObiettivo(model2, xij2);
		aggiungiVincoliMagazzino(model2, xij2);
		aggiungiVincoliDomanda(model2, xij2);
		int raggio = k - 1;
		while(raggio > 0) {
			for(int i = 0; i < m; i++) {
				for(int j = 0; j < n; j++) {
					if(mat[i][j] > raggio) {
						xij2[i][j].set(GRB.DoubleAttr.UB, 0.0);
					}
				}
			}
			model2.optimize();
			
			if(model2.get(GRB.IntAttr.Status) == GRB.Status.UNBOUNDED || model2.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
				System.out.printf("\nintervallo k = (%.4f, %.4f)\n", 0.0, (double)raggio);
				return;
			}
			raggio--;
		}
		System.out.printf("\nintervallo k = (%.4f, %.4f)\n", 0.0, (double)k);
	}

	//Metodo per trovare la soluzione ottima del duale
	private static void ottimoDuale(GRBModel model) throws GRBException {
		System.out.println("soluzione duale:");
		int i = 1;
		for(GRBConstr constr : model.getConstrs()) {
			System.out.printf("<lambda%d> = <%.4f>\n", i, constr.get(GRB.DoubleAttr.Pi));
			i++;
		}
	}
	
	//Metodo per trovare la sensitivita' della capacita' del magazzino
	private static void sensitivitaMagazzino(GRBModel model) throws GRBException {
		System.out.printf("intervallo s_%d = [%.4f, %.4f]\n", h, model.getConstr(h).get(GRB.DoubleAttr.SARHSLow), model.getConstr(h).get(GRB.DoubleAttr.SARHSUp));
	}

	//Metodo per trovare la sensitivita' della distanza cliente-magazzino
	private static void sensitivitaDistanza(GRBModel model) throws GRBException {
		GRBVar var = model.getVarByName("x_" + y + "_" + x);
		double lowerBound = var.get(GRB.DoubleAttr.SAObjLow) / c;
		double upperBound = var.get(GRB.DoubleAttr.SAObjUp) / c;
		System.out.printf("intervallo d_%d_%d = [%.4f, %.4f]\n", y, x, lowerBound, upperBound);
	}


//===========================================================================================

// QUESITO III
	
	//Metodo per aggiungere le variabili binarie
	private static GRBVar[] aggiungiY(GRBModel model) throws GRBException {
		GRBVar[] yi = new GRBVar[m];
		
		for(int i = 0; i < m; i++) {
			yi[i] = model.addVar(0, 1, 0, GRB.BINARY, "y_" + i);
		}
		return yi;
	}
	
	//Metodo per trovare la funzione obiettivo
	private static void aggiungiFunzioneObiettivo3(GRBModel model, GRBVar[] yi) throws GRBException {
		GRBLinExpr obj = new GRBLinExpr();
		
		for(int i = 0; i < alfa_j.length; i++) {
			obj.addTerm(-alfa_j[i], yi[i]);
			obj.addConstant(alfa_j[i]);
		}
		
		model.setObjective(obj);
		model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
	}

	//Metodo per aggiungere i vincoli di magazzino
	private static void aggiungiVincoliMagazzino3(GRBModel model, GRBVar[][] xij, GRBVar[] yi) throws GRBException {
	
		for(int i = 0; i < s_j.length; i++) {
		
			GRBLinExpr exprX = new GRBLinExpr();
			for(int j = 0; j < xij[0].length; j++) {
				exprX.addTerm(1, xij[i][j]);
			}
		
			GRBLinExpr exprY = new GRBLinExpr();
			exprY.addTerm(s_j[i], yi[i]);
		
			model.addConstr(exprX, GRB.LESS_EQUAL, exprY, "vincolo_produzione_i_" + i);
		} 
	}
	
	//Metodo per creare il model per il quesito III
	private static void creaModulo3(GRBEnv env) throws GRBException {
		//Creazione nuovo problema
		GRBModel model3 = new GRBModel(env);
		model3.set(GRB.IntParam.LogToConsole, 0);
		
		GRBVar[][] xij3 = aggiungiVariabili(model3);
		GRBVar[] yi = aggiungiY(model3);
		aggiungiFunzioneObiettivo3(model3, yi);
		aggiungiVincoliMagazzino3(model3, xij3, yi);
		aggiungiVincoliDomanda(model3, xij3);
		
		model3.optimize();
		
		//Risparmio ottenibile
		System.out.printf("risparmio = <%.4f>\n", model3.get(GRB.DoubleAttr.ObjVal));
		
		//Lista dei magazzini chiusi
		System.out.print("lista magazzini chiusi = [");
		for(int i = 0; i < yi.length; i++) {
			if(yi[i].get(GRB.DoubleAttr.X) == 0) {
				System.out.print(i + ", ");
			}
		}
		System.out.print("]");
		
		sfruttamentoMagazzino(model3, xij3);
		
		//Rilassamento continuo model3
		model3.relax().optimize();
		System.out.printf("rilassamento continuo = <%.4f>", model3.get(GRB.DoubleAttr.ObjVal));
	}
	
	//Metodo per individuare i magazzini meno utilizzati per inviare merci ai clienti
	private static void sfruttamentoMagazzino(GRBModel model3, GRBVar[][] xij3) throws GRBException {
		int[] merciFornite = new int[m];
		//Scopre quantita' di merci inviate per ogni magazzino
		for(int i = 0; i < m; i++) {
			for(int j = 0; j < n; j++) {
				merciFornite[i] += xij3[i][j].get(GRB.DoubleAttr.X);
			}
		}
		//Trova l'azienda che consegna meno merci
		ArrayList<Integer> listaMagazzini = new ArrayList<>();
		int min;
		min = merciFornite[0];
		for(int i = 0; i < m; i++) {
			//Esclude i magazzini chiusi
			if(merciFornite[i] == 0) {
				break;
			}
			if(merciFornite[i] == min) {
				listaMagazzini.add(i);
			}
			if(merciFornite[i] <  min) {
				min = merciFornite[i];
				listaMagazzini.clear();
				listaMagazzini.add(i);
			}
		} 
		System.out.print("\nlista magazzini meno sfruttati = [");
		for(int i = 0; i < listaMagazzini.size(); i++) {
			System.out.printf("%d", listaMagazzini.get(i));
		}
		System.out.println("]");
		}
	}