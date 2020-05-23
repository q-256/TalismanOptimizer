import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;

public class Main {
    JFrame frame;
    DisplayPanel panel_main;
    CompletionInfoPanel completionInfoPanel;
    int width = 1525;
    int height = 800;

    String rarityNames[] = {"Common","Uncommon","Rare","Epic","Legendary","Mythic"};
    String reforgeNames[] = {"Forceful", "Superior", "Strong", "Itchy", "Hurtful","Strange"};

    double damage_base = 295;
    double strength_base = 200;
    double critDmg_base = 200;
    double atcSpd_base = 30;
    double atcSpd_multiplier = 0;
    double statMultiplier = 0;
    boolean hasMastiff = false;
    boolean hasShaman = false;
    int numberOfTalismans[] = new int[]{9,11,16,9,3,1};
    int[][][] reforgeStats = new int[6][7][6];

    Calculator calculator = null;
    Thread calcThread;
    boolean stopThread = false;

    public static void main(String[] args) {
        Main main1 = new Main();
    }
    public Main(){
        generateReforgeStats();
        frame = new JFrame("Talisman Optimizer v2.1 - by q256");
        panel_main = new DisplayPanel();
        panel_main.setBounds(0,0,width,height);
        frame.setSize(width, height);
        frame.add(panel_main);
        frame.setLayout(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    void generateReforgeStats(){
        //str, cd, atc spd, def, health, speed, int
        for(int ii=0; ii<6; ii++){
            for(int jj=0; jj<7; jj++){
                reforgeStats[ii][jj] = new int[]{0,0,0,0,0,0};
            }
        }
        reforgeStats[0][0] = new int[]{4,5,7,10,15,20};

        reforgeStats[1][0] = new int[]{2,3,4,0,0,0};
        reforgeStats[1][1] = new int[]{2,2,2,0,0,0};

        reforgeStats[2][0] = new int[]{0,0,3,5,8,12};
        reforgeStats[2][1] = new int[]{0,0,3,5,8,12};
        reforgeStats[2][3] = new int[]{0,0,1,2,3,4};

        reforgeStats[3][0] = new int[]{1,1,1,2,3,4};
        reforgeStats[3][1] = new int[]{3,4,5,7,10,15};
        reforgeStats[3][2] = new int[]{0,0,1,1,1,1};

        reforgeStats[4][1] = new int[]{4,5,7,10,15,20};

        reforgeStats[5][0] = new int[]{0,1,0,3,0,4};
        reforgeStats[5][1] = new int[]{0,2,0,1,0,9};
        reforgeStats[5][2] = new int[]{0,2,0,4,0,5};
        reforgeStats[5][3] = new int[]{0,3,0,-1,0,1};

        reforgeStats[5][4] = new int[]{0,2,0,7,0,0};
        reforgeStats[5][5] = new int[]{0,0,0,0,0,3};
        reforgeStats[5][6] = new int[]{0,-1,0,0,0,11};
    }
    double getDamage(double str, double cd){
        if(hasMastiff && !hasShaman) return (damage_base+str/5) * (1+str/100) * (1+cd/200);
        else if(hasMastiff && hasShaman) return (damage_base+cd+str/5) * (1+str/100) * (1+cd/200);
        else return (damage_base+str/5) * (1+str/100) * (1+cd/100);
    }
    double getDPS(double str, double cd, double atcSpd){ return (getDamage(str, cd)*(2+atcSpd*atcSpd_multiplier/50));}

    abstract class Calculator {
        int[][] currentReforges = new int[6][6];
        int[][] bestReforges = new int[6][6];
        int bestDamage = Integer.MIN_VALUE;
        int bestDPS = Integer.MIN_VALUE;
        long totalTime = 0;
        abstract void start();
        void rate_SetUp(int[][] tempReforges){
            double str = strength_base;
            double cd = critDmg_base;
            double atcSpd = atcSpd_base;
            for(int ii=0; ii<6; ii++){
                for(int jj=0; jj<6; jj++){
                    if(tempReforges[ii][jj]!=0) {
                        str += tempReforges[ii][jj] * reforgeStats[jj][0][ii];
                        cd += tempReforges[ii][jj] * reforgeStats[jj][1][ii];
                        atcSpd += tempReforges[ii][jj] * reforgeStats[jj][2][ii];
                    }
                }
            }
            str *= 1+statMultiplier;
            cd *= 1+statMultiplier;
            atcSpd *= 1+statMultiplier;
            double damage = getDamage(str, cd);
            double DPS = getDPS(str, cd, atcSpd);
            if(DPS>bestDPS){
                bestDamage = (int)damage;
                bestDPS = (int)DPS;
                for(int ii=0; ii<6; ii++){
                    for(int jj=0; jj<6; jj++) {
                        bestReforges[ii][jj] = tempReforges[ii][jj];
                    }
                }
            }
        }
    }
    class Calculator_highestSum extends Calculator {

        public Calculator_highestSum(){
            for(int ii=0; ii<6; ii++){
                for(int jj=0; jj<6; jj++) {
                    currentReforges[ii][jj] = 0;
                }
            }
        }
        void start(){
            long startTime = System.nanoTime();
            atcSpdLoop();
            totalTime = System.nanoTime()-startTime;
            completionInfoPanel.repaint();
            panel_main.updateLabels(bestReforges);
            stopThread = false;
            calcThread = null;
        }
        void atcSpdLoop(){
            boolean exit = false;
            currentReforges[0][0] = numberOfTalismans[0];
            currentReforges[1][0] = numberOfTalismans[1];
            currentReforges[2][0] = numberOfTalismans[2];
            currentReforges[3][2] = numberOfTalismans[3];
            currentReforges[4][2] = numberOfTalismans[4];
            currentReforges[5][2] = numberOfTalismans[5];
            optimizeSetUp();
            int[][] atcSpdConversions = new int[6][3];
            atcSpdConversions[0] = new int[]{1,0,5};
            atcSpdConversions[1] = new int[]{2,0,3};
            atcSpdConversions[2] = new int[]{3,2,3};
            atcSpdConversions[3] = new int[]{3,3,5};
            atcSpdConversions[4] = new int[]{5,2,5};
            atcSpdConversions[5] = new int[]{4,2,3};

            for(int ii=0; ii<6; ii++) {
                while (!exit && currentReforges[atcSpdConversions[ii][0]][atcSpdConversions[ii][1]] > 0) {
                    int tempDPS = bestDPS;
                    currentReforges[atcSpdConversions[ii][0]][atcSpdConversions[ii][1]]--;
                    currentReforges[atcSpdConversions[ii][0]][atcSpdConversions[ii][2]]++;
                    optimizeSetUp();
                    if (bestDPS == tempDPS) exit = true;
                }
            }
        }
        void optimizeSetUp(){
            int[][] tempReforges= new int[6][6];
            for(int ii=0; ii<6; ii++){
                for(int jj=0; jj<6; jj++){
                    tempReforges[ii][jj] = currentReforges[ii][jj];
                }
            }
            int minStr = (int)strength_base+5*tempReforges[3][2]+
                    8*tempReforges[4][2]+12*tempReforges[5][2]+
                    tempReforges[1][5]+tempReforges[2][3]+
                    3*tempReforges[3][5]+2*tempReforges[3][3]+
                    3*tempReforges[4][3]+4*tempReforges[5][5];

            int maxStr = (int)strength_base+5*tempReforges[3][2]+
                    8*tempReforges[4][2]+12*tempReforges[5][2]+
                    tempReforges[1][5]+tempReforges[2][3]+
                    3*tempReforges[3][5]+2*tempReforges[3][3]+
                    3*tempReforges[4][3]+4*tempReforges[5][5]+
                    4*tempReforges[0][0]+5*tempReforges[1][0]+
                    7*tempReforges[2][0];

            int totalStrCd = (int)strength_base+(int)critDmg_base+
                    4*tempReforges[0][0]+5*tempReforges[1][0]+
                    7*tempReforges[2][0]+10*tempReforges[3][2]+
                    16*tempReforges[4][2]+24*tempReforges[5][2]+
                    3*tempReforges[1][5]+6*tempReforges[2][3]+
                    4*tempReforges[3][5]+9*tempReforges[3][3]+
                    13*tempReforges[4][3]+13*tempReforges[5][5];

            int optimalStr;

            boolean exit=false;
            while(!exit) {

                double dd = damage_base;
                double tt = totalStrCd*(1+statMultiplier);
                if(hasMastiff && !hasShaman) optimalStr = (int)((tt - 5*dd + 100 + Math.sqrt(tt*tt + 5*dd*tt + 500*tt + 25*dd*dd + 500*dd + 70000))/3/(1+statMultiplier) +0.5);
                else if(hasMastiff && hasShaman) optimalStr = (int)((9*tt + 5*dd + 400 - Math.sqrt(21*tt*tt + 30*dd*tt + 6000*tt + 25*dd*dd - 2000*dd + 1120000))/12/(1+statMultiplier) +0.5);
                else optimalStr = (int)((tt - 5*dd + Math.sqrt(25*dd*dd + tt*tt + 30000 + 5*tt*dd + 300*tt))/3/(1+statMultiplier) + 0.5);

                if (optimalStr >= minStr && optimalStr <= maxStr) {
                    tempReforges[0][4] = numberOfTalismans[0];
                    tempReforges[1][4] = numberOfTalismans[1]-tempReforges[1][5];
                    tempReforges[2][4] = numberOfTalismans[2]-tempReforges[2][3];
                    tempReforges[0][0] = 0;
                    tempReforges[1][0] = 0;
                    tempReforges[2][0] = 0;

                    int diff = optimalStr - minStr;
                    approachLargerStr(diff, tempReforges);
                    rate_SetUp(tempReforges);
                    exit = true;
                }
                else if (optimalStr < minStr) {
                    tempReforges[0][4] = numberOfTalismans[0];
                    tempReforges[1][4] = numberOfTalismans[1]-tempReforges[1][5];
                    tempReforges[2][4] = numberOfTalismans[2]-tempReforges[2][3];
                    tempReforges[0][0] = 0;
                    tempReforges[1][0] = 0;
                    tempReforges[2][0] = 0;

                    rate_SetUp(tempReforges);
                    exit = true;
                    if (tempReforges[3][2] > 0) {
                        tempReforges[3][2]--;
                        tempReforges[3][4]++;
                        minStr -= 5;
                        exit = false;
                    } else if (tempReforges[4][2] > 0) {
                        totalStrCd--;
                        tempReforges[4][2]--;
                        tempReforges[4][4]++;
                        minStr -= 7;
                        exit = false;
                    } else if (tempReforges[5][2] > 0) {
                        totalStrCd -= 4;
                        tempReforges[5][2]--;
                        tempReforges[5][4]++;
                        minStr -= 8;
                        exit = false;
                    }
                }
                else if (optimalStr > maxStr) {
                    /*tempReforges[0][0] = numberOfTalismans[0];
                    tempReforges[1][0] = numberOfTalismans[1]-tempReforges[1][5];
                    tempReforges[2][0] = numberOfTalismans[2]-tempReforges[2][3];
                    tempReforges[0][4] = 0;
                    tempReforges[1][4] = 0;
                    tempReforges[2][4] = 0;*/

                    rate_SetUp(tempReforges);
                    exit = true;
                    if (tempReforges[3][2] > 0) {
                        tempReforges[3][2]--;
                        tempReforges[3][0]++;
                        maxStr += 5;
                        exit = false;
                    } else if (tempReforges[4][2] > 0) {
                        totalStrCd--;
                        tempReforges[4][2]--;
                        tempReforges[4][0]++;
                        maxStr += 7;
                        exit = false;
                    } else if (tempReforges[5][2] > 0) {
                        totalStrCd -= 4;
                        tempReforges[5][2]--;
                        tempReforges[5][0]++;
                        maxStr += 8;
                        exit = false;
                    }
                }
            }
        }
        void approachLargerStr(int diff, int[][] tempReforges){
            // amount, ending reforge, rarity
            ArrayList<Integer[]> conversions = new ArrayList<>();
            conversions.add(new Integer[]{10, 0, 3});
            conversions.add(new Integer[]{7, 0, 2});
            conversions.add(new Integer[]{5, 2, 3});
            conversions.add(new Integer[]{5, 0, 1});
            conversions.add(new Integer[]{4, 0, 0});
            conversions.add(new Integer[]{3, 1, 1});
            conversions.add(new Integer[]{2, 1, 0});
            conversions.add(new Integer[]{1, 3, 1});
            conversions.add(new Integer[]{1, 3, 0});

            for(Integer[] ii:conversions){
                while(diff>=ii[0] && tempReforges[ii[2]][4]>0){
                    diff-=ii[0];
                    tempReforges[ii[2]][4]--;
                    tempReforges[ii[2]][ii[1]]++;
                }
            }
        }
    }

    class DisplayPanel extends JPanel{
        Color[] rarityColors = {Color.gray, Color.green, Color.blue, new Color(150, 0, 200), Color.orange, Color.cyan};
        JTextField[] inputFields = new JTextField[12];
        JLabel[] statLabels = new JLabel[9];
        JLabel[][] reforgeLabels = new JLabel[6][6];
        JCheckBox[] checkBoxes = new JCheckBox[2];


        public DisplayPanel(){
            setLayout(null);
            repaint();
            createTextFields();
            createCompletionPanel();
            createButtons();
            createLabels();
            createCheckBoxes();
        }
        public void paintComponent(Graphics g){
            Graphics2D g2d = (Graphics2D)g;
            writeStaticText(g2d);
        }
        void writeStaticText(Graphics2D g2d){
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setFont(new Font("Serif", Font.PLAIN, 24));
            g2d.drawString("Weapon Damage: ", 50, 90);
            g2d.drawString("Strength: ", 50, 120);
            g2d.drawString("Crit Damage: ", 50, 150);
            g2d.drawString("Attack Speed: ", 50, 180);
            g2d.setFont(new Font("Serif", Font.BOLD, 24));
            g2d.drawString("Stats Without Talismans:", 50, 50);
            g2d.drawString("Number of Talismans: ", 50, 250);
            g2d.drawString("Additional Settings:", 50, 540);
            for(int ii=0 ;ii<6; ii++){
                g2d.setColor(rarityColors[ii]);
                g2d.drawString(rarityNames[ii]+":", 50, 300+ii*35);
                g2d.drawString(rarityNames[ii]+":", 790, 300+ii*35);
            }
            g2d.setColor(new Color(0, 150, 0));
            g2d.setFont(new Font("Serif", Font.BOLD, 36));
            int titleLength = g2d.getFontMetrics().stringWidth("Talisman Optimizer");
            g2d.drawString("Talisman Optimizer", (width-titleLength)/2-90, 50);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine((width-titleLength)/2-90, 53, (width+titleLength)/2-90, 53);
            g2d.setColor(Color.black);
            g2d.setFont(new Font("Serif", Font.PLAIN, 24));
            g2d.drawString(" - by q256", (width+titleLength)/2-90, 50);
            g2d.drawString("v 2.1", width-70, height-50);
            for(int ii=0; ii<6; ii++){
                g2d.drawString(reforgeNames[ii], 955+ii*100-g2d.getFontMetrics().stringWidth(reforgeNames[ii])/2, 250);
            }
            g2d.drawString("Best Reforges:", 790, 200);
            g2d.drawString("Completion Info:", 525-g2d.getFontMetrics().stringWidth("Completion Info:")/2, 200);
            g2d.drawString("Total Damage:",960,50);
            g2d.drawString("Total Strength:",960,80);
            g2d.drawString("Total Crit Damage:",960,110);
            g2d.drawString("Total Attack Speed:",960,140);
            g2d.drawString("DPS:",960,170);
            g2d.drawString("Extra Health:", 1265, 50);
            g2d.drawString("Extra Defense:", 1265, 80);
            g2d.drawString("Extra Intelligence:", 1265, 110);
            g2d.drawString("Extra Speed:", 1265, 140);
            g2d.setFont(new Font("SERIF", Font.PLAIN, 20));
            g2d.drawString("+", 50, 580);
            g2d.drawString("% boost on all stats", 115, 580);
            g2d.drawString("Attack Speed multiplier:", 50, 605);
        }
        void createTextFields(){
            for(int ii=0; ii<12; ii++){
                inputFields[ii] = new JTextField("0.0");
                inputFields[ii].setBounds(230, 140+ii*35, 40, 24);
                if(ii<10 && ii>3)inputFields[ii].setText(""+numberOfTalismans[ii-4]);
                add(inputFields[ii]);
            }
            inputFields[0].setBounds(230, 70, 40,24);
            inputFields[1].setBounds(230, 100, 40,24);
            inputFields[2].setBounds(230, 130, 40,24);
            inputFields[3].setBounds(230, 160, 40,24);
            inputFields[10].setBounds(65,560,50,25);
            inputFields[11].setBounds(250,585,50,25);
            inputFields[0].setText("290");
            inputFields[1].setText("200");
            inputFields[2].setText("200");
            inputFields[3].setText("30");
            inputFields[10].setText("0.0");
            inputFields[11].setText("0.0");
        }
        void createButtons(){
            JButton calculation_button = new JButton("Calculate");
            calculation_button.setFont(new Font("Serif", Font.BOLD, 30));
            calculation_button.setBounds(400, 520, 250, 80);
            add(calculation_button);

            calculation_button.addActionListener(e -> {
                if(calcThread != null)stopThread = true;
                while(stopThread){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                retrieveInputs();
                calcThread = new Thread(() -> {
                    calculator=new Calculator_highestSum();
                    calculator.start();
                });
                calcThread.start();
            });
        }
        void retrieveInputs(){
            for(int ii=0; ii<11; ii++) {
                try {
                    if(ii<4) Double.parseDouble(inputFields[ii].getText());
                    if(ii>=4 && ii<10) if (Integer.parseInt(inputFields[ii].getText()) < 0) inputFields[ii].setText("0");
                    if(ii==10) Double.parseDouble(inputFields[10].getText());
                    if(ii==11){
                        if(Double.parseDouble(inputFields[11].getText())<0)inputFields[11].setText("0.0");
                        if(Double.parseDouble(inputFields[11].getText())>1)inputFields[11].setText("1.0");
                    }
                } catch (NumberFormatException e) {
                    switch (ii){
                        case 0: inputFields[0].setText("290"); break;
                        case 1: inputFields[1].setText("200"); break;
                        case 2: inputFields[2].setText("200"); break;
                        case 3: inputFields[3].setText("30"); break;
                        case 10: inputFields[10].setText("0.0"); break;
                        case 11: inputFields[11].setText("0.0"); break;
                        default: inputFields[ii].setText("0");
                    }
                }
            }

            statMultiplier = Double.parseDouble(inputFields[10].getText())/100;
            damage_base = Double.parseDouble(inputFields[0].getText())+5;
            strength_base = Double.parseDouble(inputFields[1].getText())/(1+statMultiplier);
            critDmg_base = Double.parseDouble(inputFields[2].getText())/(1+statMultiplier);
            atcSpd_base = Double.parseDouble(inputFields[3].getText())/(1+statMultiplier);
            atcSpd_multiplier = Double.parseDouble(inputFields[11].getText());
            for(int ii=0; ii<6; ii++){
                numberOfTalismans[ii] = Integer.parseInt(inputFields[ii+4].getText());
            }
            hasMastiff = checkBoxes[0].isSelected();
            hasShaman = checkBoxes[1].isSelected();
        }
        void createCompletionPanel(){
            completionInfoPanel = new CompletionInfoPanel();
            completionInfoPanel.setBounds(400,225, 250,250);
            add(completionInfoPanel);
        }
        void createLabels(){
            for(int ii=0; ii<9; ii++){
                statLabels[ii] = new JLabel("0");
                if(ii<5) statLabels[ii].setBounds(1155, 26+ii*30, 90, 30);
                else statLabels[ii].setBounds(1440, 26+(ii-5)*30, 90, 30);
                statLabels[ii].setFont(new Font("Serif", Font.PLAIN, 24));
                statLabels[ii].setOpaque(true);
                add(statLabels[ii]);
            }
            for(int ii=0; ii<6; ii++){
                for(int jj=0; jj<6; jj++){
                    reforgeLabels[ii][jj] = new JLabel("0");
                    reforgeLabels[ii][jj].setBounds(940+jj*100, 280+ii*35 , 60, 30);
                    reforgeLabels[ii][jj].setFont(new Font("Serif", Font.PLAIN, 24));
                    reforgeLabels[ii][jj].setOpaque(true);
                    add(reforgeLabels[ii][jj]);
                }
            }
        }
        void createCheckBoxes(){
            checkBoxes[0] = new JCheckBox("Mastiff");
            checkBoxes[1] = new JCheckBox("Shaman/Pooch");
            checkBoxes[0].setBounds(50, 615, 150, 20);
            checkBoxes[1].setBounds(50, 640, 150, 20);
            checkBoxes[0].setFont(new Font("Serif", Font.PLAIN, 20));
            checkBoxes[1].setFont(new Font("Serif", Font.PLAIN, 20));
            add(checkBoxes[0]);
            add(checkBoxes[1]);
        }
        void updateLabels(int[][] optimalReforges){
            double dmg = 0;
            double str = strength_base;
            double cd = critDmg_base;
            double ac = atcSpd_base;
            double def = 0;
            double hp = 0;
            double spd = 0;
            double intel = 0;
            for(int ii=0; ii<6; ii++){
                for(int jj=0; jj<6; jj++){
                    reforgeLabels[ii][jj].setText(""+optimalReforges[ii][jj]+"");
                    str += optimalReforges[ii][jj]*reforgeStats[jj][0][ii];
                    cd += optimalReforges[ii][jj]*reforgeStats[jj][1][ii];
                    ac += optimalReforges[ii][jj]*reforgeStats[jj][2][ii];
                    def += optimalReforges[ii][jj]*reforgeStats[jj][3][ii];
                    hp += optimalReforges[ii][jj]*reforgeStats[jj][4][ii];
                    spd += optimalReforges[ii][jj]*reforgeStats[jj][5][ii];
                    intel += optimalReforges[ii][jj]*reforgeStats[jj][6][ii];
                }
            }
            dmg = getDamage((int)(str*(1+statMultiplier)), (int)(cd*(1+statMultiplier)));
            statLabels[0].setText((int)dmg+"");
            statLabels[1].setText((int)(str*(1+statMultiplier))+"");
            statLabels[2].setText((int)(cd*(1+statMultiplier))+"");
            statLabels[3].setText((int)(ac*(1+statMultiplier))+"");
            statLabels[4].setText((int)(getDPS((int)(str*(1+statMultiplier)), (int)(cd*(1+statMultiplier)), (int)(ac*(1+statMultiplier))))+"");
            statLabels[5].setText((int)(hp*(1+statMultiplier))+"");
            statLabels[6].setText((int)(def*(1+statMultiplier))+"");
            statLabels[7].setText((int)(intel*(1+statMultiplier))+"");
            statLabels[8].setText((int)(spd*(1+statMultiplier))+"");
        }
    }
    class CompletionInfoPanel extends JPanel{
        public CompletionInfoPanel(){ setLayout(null); }
        public void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D)g;
            g2d.setColor(new Color(200,200,200));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(1,1,248,248);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setColor(Color.black);
            g2d.setFont(new Font("Serif", Font.PLAIN, 24));

            if(calculator instanceof Calculator_highestSum){
                g2d.drawString("100% completed", 125-g2d.getFontMetrics().stringWidth("100% completed")/2, 40);
                g2d.drawString("Processing Time:", 125-g2d.getFontMetrics().stringWidth("Processing Time")/2, 90);
                String time = String.format("%.6f", Math.floor(calculator.totalTime/1000)/1000000);
                g2d.drawString(time+" seconds", 125-g2d.getFontMetrics().stringWidth(time+" seconds")/2, 120);
            }
        }
    }
}
