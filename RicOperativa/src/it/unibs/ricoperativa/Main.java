package it.unibs.ricoperativa;

import java.io.*;

public class Main {

	private static int m, n, mat[][], r_i[], s_j[], c, k, alfa_j, h, x, y;
	
	public static void main(String[] args) throws IOException{
		
		readFile();

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
			case "r_i":
				r_i = new int[n];
				System.out.println();
				for(int i = 0; i < n; i++) {
					st = reader.readLine();
					stArray = st.split(" ");
					r_i[i] = Integer.parseInt(stArray[i]);
					System.out.println(r_i[i]);
				}
				break;
			case "s_j":
				s_j = new int[m];
				for(int i = 0; i < m; i++) {
					st = reader.readLine();
					stArray = st.split(" ");
					s_j[i] = Integer.parseInt(stArray[i]);
					System.out.println(s_j[i]);
				}
				break;

			}
			//Lettura della linea successiva
			st = reader.readLine();
		}
	}

}