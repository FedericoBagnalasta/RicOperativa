package it.unibs.ricoperativa;

import java.io.*;
import gurobi.*;

public class Main {

	private static int m, n, mat[][], r_i[], s_j[], k, alfa_j[], h, x, y;
	private static double c;
	
	public static void main(String[] args) throws IOException{
		
		readFile();
		try {
		//Instanza dell'ambiente di esecuzione del solver
		GRBEnv env = new GRBEnv("FileDiLog.log");
		impostaParametri(env);
		
		GRBModel model = new GRBModel(env);
		} catch (GRBException e) 
		{
			e.printStackTrace();
		}
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
	}

}