import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.*;

public class Stiri_v_vrsto implements Runnable
{
	private JFrame frame;
	private final int WIDTH = 1024;
	private final int HEIGHT = 768;
	private Painter painter;
	private Thread thread;

	private String ip;
	private int port;
	private Socket socket;
	private ServerSocket serverSocket;
	private DataOutputStream dos;
	private DataInputStream dis;
	private boolean accepted;
	private boolean inputError;
	private boolean gameStartedLAN;

	private int stVrstic = 6;
	private int stStolpcev = 7;
	private int circleArea = (int) 575 / stVrstic; // = 95
	//menu
	private int rectX = 375;
	private int rectWidth = 225;
	private int rectHeight = 50;
	private boolean play = true;
	private int turn; // turn = 0 - player igra; turn = 1 - player ne igra
	private int player; //player za LAN igro (player 1 ali 2)
	private int menu; //v kateri fazi programa smo (menu, igra, LAN igra ...)
	private int[] zmaga; //vsebuje informacije kdo je zmagal, ter koordinate za crto cez "zetone"
	private int st1; //stevec za popup menu pri LAN igri
	private int playerPoints;
	private int enemyPoints;
	private String playerName = "Igralec 1";
	private String enemyName = "Nasprotnik";
	private boolean addPlayerPoints = false;
	private boolean addEnemyPoints = false;

	private int hoverX;
	private int hoverY;

	private int[][] map = new int[stVrstic][stStolpcev];

	private int dolzinaX = map[0].length; //7
	private int dolzinaY = map.length; //6

	private String cheat = "";
	private int cheatStevec;
	private boolean cheatActivated = false;
	private boolean cheatStarted = false; //spremenljivka, da uporabnik ne more med navadno igro aktivirati kode, vendar jo mora v menuju

	//konstruktor, koda ki se izvede takoj po definiranju objekta
	public Stiri_v_vrsto()
	{
		painter = new Painter(); // Painter - class, ki je JPanel in implementa MouseListener
		painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

		frame = new JFrame("4 v vrsto");
		frame.setContentPane(painter);
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);

		thread = new Thread(this, "Stiri_v_vrsto");
		thread.start(); //klice "run()" metodo...
	}

	@Override
	public void run()
	{
		while(true)
		{
			tick();
			painter.repaint();

			try
			{
				Thread.sleep(50); //Sleep mora biti, da zmanjsa nepotrebno obremenitev procesorja
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}

		}
	}

	private void tick()
	{
		//shranim koordinate za navidezni krog
		int mouseX = MouseInfo.getPointerInfo().getLocation().x - frame.getX();
		int diferenca = ((int) ((mouseX - 130) / circleArea)) * 15; // whitespace med krogi
		int x = (int) ((mouseX - 130 - diferenca) / circleArea); // imamo tocno stevilko stolpca
		int y = preveriStolpec(x);

		if(y != -1)
		{
			hoverX = x;
			hoverY = y;
		}

		//if stavek, ki preverja "cheat"
		if(!cheatActivated)
		{
			if(cheat.equals("hesoyam"))
			{
				cheatActivated = true; //painter.repaint() tukaj ne dela, ker ta metoda le "queua" repaint...
			}

			cheatStevec++;
			if(cheatStevec == 10)
			{
				cheat = "";
				cheatStevec = 0;
			}
		}

		if(menu == 3 && turn == 1 && play && gameStartedLAN) //LAN igra, nasprotnik je na vrsti
		{
			try
			{
				int stolpec = dis.readInt();
				if(player == 1)
				{
					map[preveriStolpec(stolpec)][stolpec] = 2;
				}
				else
				{
					map[preveriStolpec(stolpec)][stolpec] = 1;
				}
				zmaga = preveriZmago();
				if(zmaga[0] != 0)
				{
					play = false;
				}
				painter.repaint();
				turn = 0;
			}
			catch(Exception e) //ce nasprotnik quita
			{
				menu = 0;
				st1 = 0; //stevec za LAN pojavno okno
				gameStartedLAN = false;
				accepted = false;
				playerPoints = 0;
				enemyPoints = 0;

				//reset mape
				for(int i = 0; i < dolzinaY; i++)
				{
					for(int j = 0; j < dolzinaX; j++)
					{
						map[i][j] = 0;
					}
				}
			}
		}

	}

	private void render(Graphics g)
	{
		if(!play) //ce je konec igre
		{
			narisiMapo(g);
			g.setFont(new Font("TimesRoman", Font.BOLD, 32));
			g.setColor(Color.black);
			if(zmaga[0] == 3) // ce je izenaceno
			{
				g.drawString("IZENA�ENO!", 410, 30);
				g.setFont(new Font("TimesRoman", Font.BOLD, 24));
				g.drawString("Kliknite za restart...", 390, 60);
			}
			else
			{
				if(menu == 1 && zmaga[0] == 2)
				{
					g.drawString("AI JE ZMAGAL!", 380, 30);
					addEnemyPoints = true;
				}
				else if(menu == 3)
				{
					if(player != zmaga[0])
					{
						g.drawString(enemyName.toUpperCase() + " JE ZMAGAL!", 330, 30);
						addEnemyPoints = true;
					}
					else
					{
						g.drawString("ZMAGA!", 450, 30);
						addPlayerPoints = true;
					}
				}
				else
				{
					g.drawString("IGRALEC " + zmaga[0] + " JE ZMAGAL!", 330, 30);
					if(zmaga[0] == 1)
					{
						addPlayerPoints = true;
					}
					else if(zmaga[0] == 2)
					{
						addEnemyPoints = true;
					}
				}

				g.setFont(new Font("TimesRoman", Font.BOLD, 24));
				g.drawString("Kliknite za restart...", 390, 60);

				g.drawString(playerName + ": " + playerPoints, 10, 25);
				g.drawString(enemyName + ": " + enemyPoints, 10, 55);

				Graphics2D g2 = (Graphics2D) g;
				g2.setStroke(new BasicStroke(4));
				// ko nekdo zmaga, se narise crta cez "zetone"
				int x = 130 + circleArea / 2;
				int y = 75 + circleArea / 2;
				g2.drawLine(zmaga[1] * circleArea + x + zmaga[1] * 15, zmaga[2] * circleArea + y + zmaga[2] * 5, zmaga[3] * circleArea + x + zmaga[3] * 15, zmaga[4] * circleArea + y + zmaga[4] * 5);
			}
		}
		else if(menu == 0)
		{
			g.setFont(new Font("TimesRoman", Font.BOLD, 35));
			g.setColor(Color.black);
			g.drawString("�TIRI V VRSTO", 377, 100);

			g.setFont(new Font("TimesRoman", Font.BOLD, 25));
			g.drawRect(rectX, 250, rectWidth, rectHeight);
			g.drawString("1 IGRALEC", 420, 285);

			g.drawRect(rectX, 350, rectWidth, rectHeight);
			g.drawString("2 IGRALCA", 420, 385);

			g.drawRect(rectX, 450, rectWidth, rectHeight);
			g.drawString("2 IGRALCA (LAN)", 388, 485);

			if(cheatActivated)
			{
				g.drawRect(rectX + rectWidth + 15, 450, rectWidth / 5, rectHeight);
				g.drawString("AI", 625, 485);
			}
		}
		else if(menu != 3 || gameStartedLAN) //igra je v poteku, izpise kdo je na vrsti...
		{
			//nastavi bel background
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, WIDTH, HEIGHT);
			narisiMapo(g);

			g.setFont(new Font("TimesRoman", Font.BOLD, 22));
			g.setColor(Color.black);
			if(menu == 3)
			{
				if(turn == 0)
				{
					g.drawString("Na vrsti si!", 460, 45);
				}
				else
				{
					g.drawString(enemyName + " je na vrsti...", 460, 45);
				}
			}
			else
			{
				g.drawString("Igralec: " + (turn + 1), 460, 45);
			}

			g.drawString(playerName + ": " + playerPoints, 10, 25);
			g.drawString(enemyName + ": " + enemyPoints, 10, 55);

		}
		else if(menu == 3 && st1 < 1) //odpri pojavno okno za vnos podatkov
		{
			JFrame frame2 = new JFrame("Vnos podatkov");
			frame2.setBounds(0, 0, 512, 384);
			frame2.setResizable(false);
			frame2.setVisible(true);
			frame2.setLocationRelativeTo(frame);
			frame2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

			JPanel panel = new JPanel();
			panel.setLayout(null);

			Font tr = new Font("TimesRoman", Font.BOLD, 22);

			JLabel labelName = new JLabel("Vnesite ime:");
			labelName.setFont(tr);
			labelName.setBounds(80, 30, 130, 20);
			JTextField t0 = new JTextField();
			t0.setBounds(210, 25, 230, 35);

			JLabel label1 = new JLabel("Vnesite IP:");
			label1.setFont(tr);
			label1.setBounds(90, 100, 130, 20);
			JTextField t1 = new JTextField();
			t1.setBounds(210, 95, 230, 35);

			JLabel label2 = new JLabel("Vnesite port:");
			label2.setFont(tr);
			label2.setBounds(75, 170, 150, 30);
			JTextField t2 = new JTextField();
			t2.setBounds(210, 165, 230, 35);

			JButton button = new JButton();
			button.setText("OK");
			button.setBounds(200, 250, 100, 50);
			button.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					inputError = false;
					String ip1 = t1.getText();
					String portString = t2.getText();
					int port1 = 0;
					try
					{
						port1 = Integer.parseInt(portString);
					}
					catch(Exception ex)
					{
						inputError = true;
						JOptionPane.showMessageDialog(frame2, "Narobe, vnesite ponovo:");
					}

					if(ip1.length() > 15 || port1 < 1024 || port1 > 65535)
					{
						inputError = true;
						JOptionPane.showMessageDialog(frame2, "Narobe, vnesite ponovo:");
					}

					String[] ipTabela = ip1.split("\\.");
					for(int i = 0; i < ipTabela.length; i++)
					{
						int ipOctet = Integer.parseInt(ipTabela[i]);
						if(ipOctet > 255)
						{
							inputError = true;
							JOptionPane.showMessageDialog(frame2, "Narobe, vnesite ponovo:");
						}
					}

					if(!inputError) // ce ni error-ja, nadaljuj igro
					{
						ip = ip1;
						port = port1;

						if(!t0.getText().equals("")) //ce polje za ime playerja ni prazno
						{
							playerName = t0.getText();
						}

						frame2.dispose();
						if(!connect()) //ce ne connecta...
						{
							initializeServer(); //...naredi server
							System.out.println("Cakam clienta ...");
						}
						while(!accepted)
						{
							listenForServerRequest();
						}

						// dobi ime nasprotnika
						try
						{
							dos.writeUTF(playerName);
						}
						catch(Exception e1)
						{
							e1.printStackTrace();
						}

						try
						{
							enemyName = dis.readUTF();
							if(enemyName.equals("Igralec 1"))
							{
								enemyName = "Nasprotnik";
							}
						}
						catch(IOException e2)
						{
							e2.printStackTrace();
						}

						gameStartedLAN = true;
						painter.repaint();
					}
				}
			});

			panel.add(labelName);
			panel.add(t0);
			panel.add(label1);
			panel.add(t1);
			panel.add(label2);
			panel.add(t2);
			panel.add(button);

			frame2.add(panel);
			st1++;
		}

		//narisi menu button
		if(menu != 0)
		{
			g.setFont(new Font("TimesRoman", Font.BOLD, 25));
			g.drawRect(885, 10, 120, rectHeight);
			g.drawString("MENU", 907, 45);
		}

		//narisi mouse hover
		if(menu != 0 && play)
		{
			if(menu != 3) //ne gre za LAN igro
			{
				if(turn == 0)
				{
					g.setColor(new Color(0, 255, 0, 170));
					g.fillOval(hoverX * circleArea + 130 + hoverX * 15, hoverY * circleArea + 75 + hoverY * 5, circleArea, circleArea);
				}
				else
				{
					g.setColor(new Color(255, 0, 0, 170));
					g.fillOval(hoverX * circleArea + 130 + hoverX * 15, hoverY * circleArea + 75 + hoverY * 5, circleArea, circleArea);
				}
			}
			//LAN igra
			else if(player == 1 && gameStartedLAN && turn == 0 && !cheatActivated) //player 1 ima zeleno barvo
			{
				g.setColor(new Color(0, 255, 0, 170));
				g.fillOval(hoverX * circleArea + 130 + hoverX * 15, hoverY * circleArea + 75 + hoverY * 5, circleArea, circleArea);
			}
			else if(gameStartedLAN && turn == 0 && !cheatActivated) //player 2 ima rdeco barvo
			{
				g.setColor(new Color(255, 0, 0, 170));
				g.fillOval(hoverX * circleArea + 130 + hoverX * 15, hoverY * circleArea + 75 + hoverY * 5, circleArea, circleArea);
			}
		}
	}

	private int preveriStolpec(int x)
	{
		if(x >= 0 && x < dolzinaX)
		{
			int y = 0;
			while(map[y][x] == 0) //gre od zgoraj navzdol
			{
				if(y == dolzinaY - 1 || map[y + 1][x] != 0) //ce je prisel do konca ali je naslednje polje zasedeno
				{
					return y;
				}
				y++;
			}
		}

		return -1;
	}

	private void narisiMapo(Graphics g)
	{
		for(int i = 0; i < dolzinaY; i++)
		{
			for(int j = 0; j < dolzinaX; j++)
			{
				// zacnem pri (130,75); padding po x: 15; padding po y: 5
				if(map[i][j] == 1) //ce je zeton na polju zasedel player 1 - zelen zeton
				{
					g.setColor(Color.green);
					g.fillOval(j * circleArea + 130 + j * 15, i * circleArea + 75 + i * 5, circleArea, circleArea);
				}
				else if(map[i][j] == 2) //ce je zeton na polju zasedel player 2 - rdec zeton
				{
					g.setColor(Color.red);
					g.fillOval(j * circleArea + 130 + j * 15, i * circleArea + 75 + i * 5, circleArea, circleArea);
				}
				else if(cheatActivated && player == 1) //ce player cheat-a
				{
					g.setColor(new Color(0, 255, 0, 60));
					g.fillOval(j * circleArea + 130 + j * 15, i * circleArea + 75 + i * 5, circleArea, circleArea);
				}
				else if(cheatActivated && player == 2) //ce drugi player cheat-a
				{
					g.setColor(new Color(255, 0, 0, 60));
					g.fillOval(j * circleArea + 130 + j * 15, i * circleArea + 75 + i * 5, circleArea, circleArea);
				}
				else if(map[i][j] == 0) //ce je polje prazno
				{
					g.setColor(Color.black);
				}
				
				g.drawOval(j * circleArea + 130 + j * 15, i * circleArea + 75 + i * 5, circleArea, circleArea);
				
			}
		}
	}

	public int[] preveriZmago()
	{
		String t = "";
		int stevec = 0;
		int vsota1 = 0;
		int vsota2 = 0;

		for(int i = 0; i < dolzinaY; i++)
		{
			for(int j = 0; j < dolzinaX; j++)
			{
				t += map[i][j]; // v string t shranim vse vrednosti iz tabele
			}
			t += " ";
		}

		//preveri izenacitev
		for(int i = 0; i < t.length(); i++)
		{
			if(t.charAt(i) == '0')
			{
				break;
			}
			if(i == t.length() - 1)
			{
				return new int[] {3}; //izenaceno
			}
		}

		//preveri vrstico
		if(t.contains("1111") || t.contains("2222"))
		{
			int index = 0;
			int p = 0;
			if(t.contains("1111"))
			{
				index = t.indexOf("1111");
				p = 1;
			}
			else
			{
				index = t.indexOf("2222");
				p = 2;
			}
			int dolzina = dolzinaX + 1; // + 1, ker po vsaki vrstici damo presledek " "
			return new int[] {p, index % dolzina, index / dolzina, (index % dolzina) + 3, index / dolzina}; // player, x1, y1, x2, y2
		}

		//preveri stolpec
		for(int x = 0; x < dolzinaX; x++)
		{
			int y = preveriStolpec(x) + 1;

			//postavi se na prvi zeton v stolpcu
			if(y != dolzinaY)
			{
				int prviZeton = map[y][x];
				for(int i = y; i < dolzinaY; i++)
				{
					if(map[i][x] == prviZeton) //ce se zeton igralca ni spremnil...
					{
						vsota1 += map[i][x]; //... polni vrednost vsote
					}
					else
					{
						break; //ce se je zeton spremenil, takoj prekine, saj zmaga ni mozna
					}

					if(vsota1 % 4 == 0 && vsota1 != 0 && (y + 3) <= i) //ce je nekdo zmagal in je zanka sla vsaj 4 krat
					{
						return new int[] {vsota1 / 4, x, y, x, y + 3}; //kateri igralec je zmagal, x1 koord, y1, x2, y2
					}
				}
				vsota1 = 0;
			}
		}

		//preveri SPODNJI del mape diagonalno
		for(int i = dolzinaY - 4; i >= 0; i--)
		{
			stevec = 0;
			while(dolzinaY - i - stevec >= 4) //poskrbi, da gre cez vsa polja
			{
				for(int j = 0; j < 4; j++)
				{
					//od desne proti levi dol
					if(map[i + j + stevec][dolzinaX - 1 - j - stevec] != 0)
					{
						vsota1 += map[i + j + stevec][dolzinaX - 1 - j - stevec];
					}
					else
					{
						vsota1 = -10; //ce je v polju 0, te vsote ne smem upostevati, vsota nikoli ne bo vecja od 8 (2*4)
					}

					//od leve proti desni dol
					if(map[i + j + stevec][j + stevec] != 0)
					{
						vsota2 += map[i + j + stevec][j + stevec];
					}
					else
					{
						vsota2 = -10; //ce je v polju 0, te vsote ne smem upostevati (2202 - ni zmaga)
					}
				}
				if(vsota1 % 4 == 0 && vsota1 > 0)
				{
					//player, x1, y1, x2, y2
					return new int[] {map[i + stevec][dolzinaX - 1 - stevec], dolzinaX - 1 - stevec, i + stevec, dolzinaX - 1 - stevec - 3, i + stevec + 3};
				}
				if(vsota2 % 4 == 0 && vsota2 > 0)
				{
					//player, x1, y1, x2, y2
					return new int[] {map[i + stevec][stevec], stevec, i + stevec, stevec + 3, i + stevec + 3};
				}

				vsota1 = 0;
				vsota2 = 0;
				stevec++;
			}
		}

		//preveri ZGORNJI del mape diagonalno
		for(int i = 1; i <= dolzinaX - 4; i++)//vodoravno zgoraj
		{
			stevec = 0;
			while(dolzinaX - i - stevec >= 4) //poskrbi, da gre cez vsa polja
			{
				for(int j = 0; j < 4; j++)
				{
					//od leve proti desni dol
					if(map[j + stevec][j + i + stevec] != 0)
					{
						vsota1 += map[j + stevec][i + j + stevec];
					}
					else
					{
						vsota1 = -10; //ce je v polju 0, te vsote ne smem upostevati, vsota nikoli ne bo vecja od 8 (2*4), zato bo tukaj vedno negativna
					}

					//od desne proti levi dol
					if(map[j + stevec][dolzinaX - 1 - i - j - stevec] != 0)
					{
						vsota2 += map[j + stevec][dolzinaX - 1 - i - j - stevec];
					}
					else
					{
						vsota2 = -10; //ce je v polju 0, te vsote ne smem upostevati, vsota nikoli ne bo vecja od 8 (2*4), zato bo tukaj vedno negativna
					}
				}

				//ne sme biti v istem if stavku, saj se koordinate crte razlikujejo
				if(vsota1 % 4 == 0 && vsota1 > 0)
				{
					//player, x1, y1, x2, y2
					return new int[] {map[stevec][i + stevec], i + stevec, stevec, i + stevec + 3, stevec + 3};
				}

				if(vsota2 % 4 == 0 && vsota2 > 0)
				{
					//player, x1, y1, x2, y2
					return new int[] {map[stevec][dolzinaX - 1 - i - stevec], dolzinaX - 1 - i - stevec, stevec, dolzinaX - 1 - i - stevec - 3, stevec + 3};
				}

				vsota1 = 0;
				vsota2 = 0;
				stevec++;
			}
		}

		return new int[] {0}; // ce vrne 0, nobeden ni zmgal
	}

	public int pcTurn(int[][] map)
	{
		int output = -1;
		int y = 0;
		int vrednost = 0;
		int vrednostPrej = 0; // vrednostPrej drzi prejsnjo vrednost "vrednosti"

		for(int i = 0; i < dolzinaX; i++)
		{
			y = preveriStolpec(i);
			if(y != -1)
			{
				vrednost = 0;
				for(int j = 0; j < 2; j++) //zanka, da preveri se en stoplec dalje
				{
					if(y - j < 0) //ce je izven mape
					{
						break;
					}
					vrednost += izracunPoteze(i, y - j, j);
				}
			}

			if(vrednost > vrednostPrej && y != -1)
			{
				output = i;
				vrednostPrej = vrednost;
			}
		}

		//ce ni uspel dobiti vrednosti "vzame" prvi mozni stolpec
		if(output == -1)
		{
			for(int i = 0; i < dolzinaX; i++)
			{
				y = preveriStolpec(i);
				if(y != -1)
				{
					return i;
				}
			}
		}

		return output;
	}

	private int izracunPoteze(int x, int y, int step) //AI PREVERJA 2 KORAKA VNAPREJ - step
	{
		//sredina = +5; vrstica dveh = 2; vrstica treh = 9; zmaga = +2000;
		int output = 0;
		int zeton = 0;
		int vrednost = 0;

		//ce je na sredini (7 / 2 = 3.5 => 3)
		if(x == dolzinaX / 2)
		{
			output += 5;
		}

		//Math.max, Math.min - edge cases, zagotovi da ne gre v negativno ali izven mape

		// preveri levo in desno polje (x+1, x-1)
		if(map[y][Math.min(x + 1, dolzinaX - 1)] != 0 || map[y][Math.max(x - 1, 0)] != 0)
		{
			output++;
		}

		// preveri vrstico
		for(int i = Math.max(x - 3, 0); i <= Math.min((x + 3), dolzinaX - 1); i++)//zacne od 3 manj, konca pri 3 vec (od x)
		{
			if(map[y][i] != 0 && (map[y][i] == zeton || zeton == 0)) //ce se lestvica nadaljuje ali se je zacela
			{
				//v tem koraku je dano polje zasedeno (1 ali 2)
				zeton = map[y][i]; // shranim vrednost, da jo lahko preverim lestvico v naslednjem koraku
				vrednost++;
			}
			else if(map[y][i] != 0 && i > x) // ce se lestivica ne nadaljuje po zacetnem x (vse kar se nadaljuje ni pomembno)
			{
				break;
			}
			else if(map[y][i] != 0) // ce se lestivica ne nadaljuje (2212 - pri enki se lestvica resetira)
			{
				zeton = map[y][i]; // prejsnja vrednost je trenutni "zeton" (da lahko nadaljujem lestvico)
				vrednost = 1; // nastavim na 1, saj imam ze lestvico z 1 "zetonom"
			}
		}

		//ce preverja eno potezo vnaprej vrednost zmanjsam, da ne pomaga playerju
		if(step == 1)
		{
			vrednost *= -0.4;
		}
		else
		{
			//manipuliram vrednost
			if(vrednost == 3)
			{
				vrednost = 1000 * zeton;
			}
			else
			{
				vrednost = (int) Math.pow(3, vrednost + 1); //+1, ker je vrstica vec vredna
			}
		}

		output += vrednost; //"evaluiram" vrednost trenutnega polja
		vrednost = 0;
		zeton = 0;

		// preveri stolpec
		if(step < 1) //stolpec preverjam samo v prvem koraku, ce ne pristejem 2 krat isto vrednost po nepotrebnem
		{
			for(int i = y; i < dolzinaY; i++) // j se mora zaceti na y, ker gre navzdol (navzgor ni nobenega zetona...)
			{
				if(map[i][x] != 0 && (map[i][x] == zeton || zeton == 0)) //ce se lestvica nadaljuje ali se je zacela
				{
					zeton = map[i][x]; // shranim vrednost, da lahko preverim lestvico v naslednjem koraku
					vrednost++;
					if(vrednost == 3) //ce je zaporedje treh, mora breakat da se loop ne nadaljuje (da ne shrani naslednjega zetona)
					{
						break;
					}
				}
				else if(map[i][x] != 0) // ce se lestivica ne nadaljuje (vse kar se nadaljuje je NEPOMEMBNO, saj preverjam navzdol)
				{
					break;
				}
			}
			//tukaj ne nastavim vrednosti na negativno, saj je stolpec...

			//manipuliram vrednost
			if(vrednost == 3)
			{
				vrednost = 1000 * zeton;
			}
			else
			{
				vrednost = (int) Math.pow(3, vrednost) - 1;
			}

			output += vrednost;
			zeton = 0;
			vrednost = 0;
		}

		// preveri diagonalno: /, OD SPODAJ GOR
		//izracun do robov mape (ce ne break-a, konca pri 4) - SLIKA
		int razlika = 0;
		for(int i = 1; i <= 4; i++)
		{
			if(y + i <= dolzinaY - 1 && x - i >= 0) //gre do obeh koncev mape (po x in y)
			{
				razlika++;
			}
			else
			{
				break;
			}
		}

		//diagonala je dolga 3, ALI za vrednost Y, ALI vrednost dolzinaX-1-x
		for(int i = -razlika; i < 4 && i <= y && i <= dolzinaX - 1 - x; i++) //takoj, ko je en FALSE, se loop prekine
		{
			int checkY = Math.min(y - i, dolzinaY - 1);
			int checkX = x + i;

			if(map[checkY][checkX] != 0 && (zeton == 0 || zeton == map[checkY][checkX]))
			{
				vrednost++;
				if(vrednost == 3) //ce je zaporedje treh, mora breakat da se loop ne nadaljuje (da ne shrani naslednjega zetona)
				{
					break;
				}
			}
			else if(map[checkY][checkX] != 0)
			{
				vrednost = 1; // nastavim na 2, saj imam ze lestvico z 1 "zetonom"
			}

			zeton = map[checkY][checkX]; //shrani trenutni zeton
		}

		//ce preverja eno potezo vnaprej vrednost zmanjsam, da ne pomaga playerju
		if(step == 1)
		{
			vrednost *= -0.4;
		}
		else
		{
			//manipuliram vrednost
			if(vrednost == 3)
			{
				vrednost = 1000 * zeton;
			}
			else
			{
				vrednost = (int) Math.pow(3, vrednost) - 1;
			}
		}

		output += vrednost;
		zeton = 0;
		vrednost = 0;

		// preveri diagonalno: \
		//izracun do robov mape (ce ne break-a, konca pri 4) - SLIKA
		razlika = 0;
		for(int i = 1; i <= 4; i++)
		{
			if(y + i <= dolzinaY - 1 && x + i <= dolzinaX - 1) //gre do obeh koncev mape (po x in y)
			{
				razlika++;
			}
			else
			{
				break;
			}
		}

		//diagonala je dolga 3 ALI za vrednost Y ALI vrednost dolzinaX-1-x
		for(int i = -razlika; i < 4 && i <= y && i <= x; i++) //takoj, ko je en FALSE, se loop prekine
		{
			int checkY = Math.min(y - i, dolzinaY - 1);
			int checkX = x - i;

			if(map[checkY][checkX] != 0 && (zeton == 0 || zeton == map[checkY][checkX]))
			{
				vrednost++;
				if(vrednost == 3) //ce je zaporedje treh, mora breakat da se loop ne nadaljuje (da ne shrani naslednjega zetona)
				{
					break;
				}
			}
			else if(map[checkY][checkX] != 0)
			{
				vrednost = 1; // nastavim na 1, saj imam ze lestvico z 1 "zetonom"
			}

			zeton = map[checkY][checkX]; //shrani trenutni zeton
		}

		//ce preverja eno potezo vnaprej vrednost postane negativna, da ne pomaga playerju
		if(step == 1)
		{
			vrednost *= -0.4;
		}
		else
		{
			//manipuliram vrednost
			if(vrednost == 3)
			{
				vrednost = 1000 * zeton;
			}
			else
			{
				vrednost = (int) Math.pow(3, vrednost) - 1;
			}
		}

		output += vrednost;
		
		return output;
	}

	private boolean connect()
	{
		try
		{
			socket = new Socket(ip, port);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			accepted = true;
		}
		catch(IOException e)
		{
			System.out.println("Ne morem povezati na " + ip + " startam server...");
			return false;
		}
		turn = 1;
		player = 2;
		System.out.println("Povezan na server!");
		return true;
	}

	private void initializeServer()
	{
		try
		{
			serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		turn = 0;
		player = 1;
	}

	public void listenForServerRequest()
	{
		try
		{
			socket = serverSocket.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			accepted = true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		Stiri_v_vrsto stiri_v_vrsto = new Stiri_v_vrsto();
	}

	private class Painter extends JPanel implements MouseListener, KeyListener
	{
		private static final long serialVersionUID = 1L;

		public Painter()
		{
			setFocusable(true);
			requestFocus();
			setBackground(Color.WHITE);
			addMouseListener(this);
			addKeyListener(this);
		}

		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			render(g);
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			//ce klikne button za menu
			if(menu != 0)
			{
				if(e.getX() >= 885 && e.getX() <= 885 + 120 && e.getY() >= 10 && e.getY() <= 10 + rectHeight)
				{
					if(menu == 3)
					{
						try
						{
							if(player == 1) //server
							{
								if(serverSocket != null)
								{
									serverSocket.close();
								}
								serverSocket = null;
							}

							if(socket != null)
							{
								socket.close();
							}
							socket = null;
						}
						catch(IOException e1)
						{
							e1.printStackTrace();
						}
					}

					menu = 0;
					st1 = 0; //stevec za LAN pojavno okno
					gameStartedLAN = false;
					accepted = false;
					playerPoints = 0;
					enemyPoints = 0;
					turn = 0;

					//reset mape
					for(int i = 0; i < dolzinaY; i++)
					{
						for(int j = 0; j < dolzinaX; j++)
						{
							map[i][j] = 0;
						}
					}
				}
			}

			//restart
			if(!play)
			{
				if(addPlayerPoints)
				{
					playerPoints++;
				}
				if(addEnemyPoints)
				{
					enemyPoints++;
				}

				addPlayerPoints = false;
				addEnemyPoints = false;

				if(menu == 3)
				{
					try
					{
						dos.writeUTF("Igralec " + player + " je ready.");
					}
					catch(Exception e1)
					{
						e1.printStackTrace();
					}

					try
					{
						dis.readUTF();
					}
					catch(IOException e2)
					{
						e2.printStackTrace();
					}
				}
				play = true;
				
				// bug fix...
				if(menu == 1)
				{
					turn = 0;
				}

				//reset mape
				for(int i = 0; i < dolzinaY; i++)
				{
					for(int j = 0; j < dolzinaX; j++)
					{
						map[i][j] = 0;
					}
				}
			}
			else
			{
				if(menu == 0) //ce smo v menuju
				{
					if(e.getX() >= rectX && e.getX() <= rectX + rectWidth)
					{
						if(e.getY() >= 250 && e.getY() <= 250 + rectHeight)
						{
							menu = 1;
							enemyName = "AI";
							cheatActivated = false;
						}
						else if(e.getY() >= 350 && e.getY() <= 350 + rectHeight)
						{
							menu = 2;
							enemyName = "Igralec 2";
							cheatActivated = false;
						}
						else if(e.getY() >= 450 && e.getY() <= 450 + rectHeight)
						{
							menu = 3;
							cheatActivated = false;
							cheatStarted = false;
						}
					}
					//ce klikne "AI-cheat" button
					else if(e.getX() >= rectX + rectWidth + 15 && e.getX() <= rectX + rectWidth + 15 + rectWidth / 5 && cheatActivated)
					{
						if(e.getY() >= 450 && e.getY() <= 450 + rectHeight)
						{
							menu = 3;
							cheatStarted = true;
						}

					}
				}
				//lokalna igra
				else if(menu != 3 && play)
				{
					int diferenca = ((int) ((e.getX() - 130) / circleArea)) * 15; // whitespace med krogi
					int x = (int) ((e.getX() - 130 - diferenca) / circleArea); // imamo tocno stevilko stolpca
					int y = preveriStolpec(x);

					if(y != -1)
					{
						if(turn == 0) // igra player 1
						{
							map[y][x] = 1;
						}
						else if(turn == 1 && menu == 2) // igra player 2
						{
							map[y][x] = 2;
						}
						
						turn = (turn + 1) % 2;
						zmaga = preveriZmago();
						if(zmaga[0] != 0)
						{
							play = false;
						}

						if(turn == 1 && menu == 1 && play) // igra pc
						{
							int pcX = pcTurn(map); //izra�una kateri stolpec bo igral
							map[preveriStolpec(pcX)][pcX] = 2; //vnesem v tabelo

							turn = (turn + 1) % 2;
						}
					}
				}
				//LAN igra
				else if(menu == 3 && play && gameStartedLAN && accepted && turn == 0)
				{
					int diferenca = ((int) ((e.getX() - 130) / circleArea)) * 15; // whitespace med krogi
					int x = (int) ((e.getX() - 130 - diferenca) / circleArea); // imamo tocno stevilko stolpca
					int y = preveriStolpec(x);
					
					if(cheatActivated)//namesto igralca igra PC
					{
						x = pcTurn(map); // dobim index stolpca
						y = preveriStolpec(x); // dobim index vrstice
					}

					if(y != -1)
					{
						map[y][x] = player;
						turn = 1;
						Toolkit.getDefaultToolkit().sync();

						try
						{
							dos.writeInt(x);
							dos.flush();
						}
						catch(Exception e1) //ce igralec prekine igro
						{
							if(player == 1) //ce je server
							{
								try
								{
									socket.close();
									serverSocket.close();

									socket = null;
									serverSocket = null;
								}
								catch(IOException e2)
								{
									e2.printStackTrace();
								}
							}
							//e1.printStackTrace();
						}
					}
				}

				zmaga = preveriZmago();
				if(zmaga[0] != 0)
				{
					play = false;
				}
			}

			painter.repaint();
		}

		@Override
		public void mouseEntered(MouseEvent arg0)
		{
		}

		@Override
		public void mouseExited(MouseEvent arg0)
		{
		}

		@Override
		public void mouseClicked(MouseEvent arg0)
		{
		}

		@Override
		public void mousePressed(MouseEvent arg0)
		{
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
			//KeyListener za cheat (hesoyam)
			if(cheatStarted || menu == 0)
			{
				cheat += e.getKeyChar() + "";
				cheatStevec = 0;
				
				if(e.getKeyChar() == 'x')
				{
					cheat = "";
					cheatActivated = false;
					painter.repaint();
				}
			}
		}

		@Override
		public void keyPressed(KeyEvent e)
		{
		}

		@Override
		public void keyTyped(KeyEvent e)
		{
		}

	}

}