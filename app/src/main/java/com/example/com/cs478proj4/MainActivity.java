package com.example.com.cs478proj4;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity{
    //vars used for game stats, progress, etc.
    private int turn = 1, piecesLeft1 = 3, piecesLeft2 = 3;
    private boolean gameInProgress = false, gameOver = false;
    private int backBoard[][] = new int[3][3];

    //register UI views
    private Button startStopButton, toastButton;
    private ImageButton frontBoard[][] = new ImageButton[3][3];

    //handler messages
    public static final int WAIT = 1;               //used by worker threads
    public static final int MAKE_MOVE = 2;          //used by worker threads
    public static final int GAME_OVER = 3;          //used by worker threads

    public static final int DONE_WAITING = 5;       //used by ui thread
    public static final int UPDATE_BOARD = 6;       //used by ui thread
    public static final int NEXT_MOVE = 7;          //used by ui thread

    //handler associated with UI thread
    private class HandlerUI extends Handler{
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Message m;
            switch (what) {
                //updates the game board, and checks if the game needs to be continued (if there is a winner or not)
                case UPDATE_BOARD:
                    updateShowBoard();
                    int winner = checkWinner();

                    //continue playing the game if the game is no worker has won
                    if(winner == -1){
                        System.out.println("No winner");
                        m = handlerUI.obtainMessage(NEXT_MOVE);
                        handlerUI.sendMessage(m);
                    }
                    //if either has one, stop both threads and display winner
                    else if(winner == 1){
                        gameInProgress = false;
                        Toast.makeText(MainActivity.this, "Circles win", Toast.LENGTH_SHORT).show();
                        m = worker1Thread.worker1Handler.obtainMessage(GAME_OVER);
                        worker1Thread.worker1Handler.sendMessage(m);
                        m = worker2Thread.worker2Handler.obtainMessage(GAME_OVER);
                        worker2Thread.worker2Handler.sendMessage(m);
                    }
                    else if(winner == 2){
                        gameInProgress = false;
                        Toast.makeText(MainActivity.this, "Crosses win", Toast.LENGTH_SHORT).show();
                        m = worker2Thread.worker2Handler.obtainMessage(GAME_OVER);
                        worker2Thread.worker2Handler.sendMessage(m);
                        m = worker1Thread.worker1Handler.obtainMessage(GAME_OVER);
                        worker1Thread.worker1Handler.sendMessage(m);
                    }

                    break;

                //check which worker is to make a move, and send a message to the worker to make a move
                case NEXT_MOVE:
                    //check which worker is to make a move
                    if(turn % 2 == 1){
                        m = worker1Thread.worker1Handler.obtainMessage(MAKE_MOVE);
                        worker1Thread.worker1Handler.sendMessage(m);
                    }
                    else if(turn % 2 == 0){
                        m = worker2Thread.worker2Handler.obtainMessage(MAKE_MOVE);
                        worker2Thread.worker2Handler.sendMessage(m);
                    }
                    break;
            }//end switch(...)
        }//end handleMessage(...)
    }//end HandlerUI class


    //threads and handlers
    public HandlerUI handlerUI;
    public Worker1Thread worker1Thread;
    public Worker2Thread worker2Thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeBoard();
        updateShowBoard();

        //initialize threads and handlers
        handlerUI = new HandlerUI();
        worker1Thread = new Worker1Thread(handlerUI);
        worker2Thread = new Worker2Thread(handlerUI);
        worker1Thread.start();
        worker2Thread.start();

        //initialize buttons and listeners
        startStopButton = (Button) findViewById(R.id.startStopButton);
        toastButton = (Button) findViewById(R.id.toastButton);
        startStopButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                /*
                if(!gameInProgress){
                    gameInProgress = true;
                    Toast.makeText(MainActivity.this, "Starting Game", Toast.LENGTH_SHORT).show();
                    playGame();
                }
                else {
                    Toast.makeText(MainActivity.this, "Restarting Game", Toast.LENGTH_SHORT).show();
                    resetGame(1);
                }*/

                if(!gameOver) {
                    if(gameInProgress){
                        resetGame(1);
                    }
                    else{
                        playGame();
                        gameInProgress = true;
                    }
                }
                if(gameOver){
                    Toast.makeText(MainActivity.this, "Game over", Toast.LENGTH_SHORT).show();
                    resetGame(1);
                }


            }
        });//end start/stop listener
        
        toastButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Toast.makeText(MainActivity.this, "Toastyyy", Toast.LENGTH_SHORT).show();
            }
        });//end toast listener

    }//end onCreate(...)

    //stop the threads when the program exits
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopThreads();
    }//end onDestroy()

    //gets a reference to the ImageButton objects, and sets the images to be blank, initializes back-end frontBoard
    public void initializeBoard(){
        frontBoard[0][0] = (ImageButton) findViewById(R.id.TL);
        frontBoard[0][1] = (ImageButton) findViewById(R.id.TM);
        frontBoard[0][2] = (ImageButton) findViewById(R.id.TR);

        frontBoard[1][0] = (ImageButton) findViewById(R.id.ML);
        frontBoard[1][1] = (ImageButton) findViewById(R.id.MM);
        frontBoard[1][2] = (ImageButton) findViewById(R.id.MR);

        frontBoard[2][0] = (ImageButton) findViewById(R.id.BL);
        frontBoard[2][1] = (ImageButton) findViewById(R.id.BM);
        frontBoard[2][2] = (ImageButton) findViewById(R.id.BR);

        for(int i = 0; i < 3; i++)
            for(int j = 0; j < 3; j++)
                backBoard[i][j] = -1;
    }//end initializeBoard()

    //gathers data from the back end frontBoard, updates the front end frontBoard, and updates the display
    public void updateShowBoard(){
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                frontBoard[i][j].setVisibility(View.VISIBLE);
                if(backBoard[i][j] == -1)
                    frontBoard[i][j].setImageResource(R.drawable.blank);
                else if(backBoard[i][j] == 0)
                    frontBoard[i][j].setImageResource(R.drawable.circle);
                else if(backBoard[i][j] == 1)
                    frontBoard[i][j].setImageResource(R.drawable.cross);
            }
        }

        System.out.println(backBoard[0][0] + " " + backBoard[0][1] + " " + backBoard[0][2]);
        System.out.println(backBoard[1][0] + " " + backBoard[1][1] + " " + backBoard[1][2]);
        System.out.println(backBoard[2][0] + " " + backBoard[2][1] + " " + backBoard[2][2]);
    }//end updateShowBoard()

    //reinitialize board and restarts the workers
    public void resetGame(int i){
        stopThreads();
        turn = 1;
        gameInProgress = true;
        gameOver = false;
        piecesLeft1 = 3;
        piecesLeft2 = 3;
        initializeBoard();
        updateShowBoard();


        playGame();
    }//end resetGame();

    //starts the two worker threads, and initiates moves one worker at a time until the game is finished
    public void playGame(){
        Message m = worker1Thread.worker1Handler.obtainMessage(MAKE_MOVE);
        //send message to worker1 thread, and have worker1 handler take care of message
        worker1Thread.worker1Handler.sendMessage(m);
    }//end playGame()

    //returns 2 if cross wins, returns 1 if circle wins, returns -1 if neither wins
    public int checkWinner(){
        int crossCounter = 0, circleCounter = 0;

        //check horizontals
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                if(backBoard[i][j] == 1)
                    crossCounter++;
                else if(backBoard[i][j] == 0)
                    circleCounter++;

                //System.out.println("Horizontal: " + i + "," + j);
            }

            if(crossCounter == 3)
                return 2;
            else if(circleCounter == 3)
                return 1;

        }

        crossCounter = 0;
        circleCounter = 0;

        //check verticals
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                if(backBoard[j][i] == 1)
                    crossCounter++;
                else if(backBoard[j][i] == 0)
                    circleCounter++;

                //System.out.println("Vertical: " + i + "," + j);
            }
            if(crossCounter == 3)
                return 1;
            else if(circleCounter == 3)
                return 2;
        }
        return -1;
    }//end checkWinner()

    //move for worker1
    public void makeMove1(){
        System.out.println("Worker 1 Turn: " + turn);
        turn++;
        boolean found = false;
        //if there are outstanding pieces remaining, add to the board from the top left most open spot
        if(piecesLeft1 > 0){
            for(int i = 0; i < 3; i++){
                for(int j = 0; j < 3; j++){
                    if(!found) {
                        if (backBoard[i][j] == -1) {
                            backBoard[i][j] = 0;
                            found = true;
                            //updateShowBoard();
                        }
                    }
                }
            }
            piecesLeft1--;
            System.out.println("Pieces left: " + piecesLeft1);
        }
        //finds the top left most open spot, and moves the bottom right most occupied slot to the open slot;
        else{
            System.out.println("Out of pieces");
            int rowToPlace = -1, colToPlace = -1;

            //search for open
            for(int i = 0; i < 3; i++){
                for(int j = 0; j < 3; j++){
                    if(!found) {
                        if (backBoard[i][j] == -1) {
                            rowToPlace = i;
                            colToPlace = j;
                            found = true;
                        }
                    }
                }
            }

            found = false;

            //search for occupied until it is found, and set it to blank
            for(int i = 2; i >= 0; i--){
                for(int j = 2; j >= 0; j--){
                    if(!found) {
                        if (backBoard[i][j] == 0) {
                            backBoard[i][j] = -1;
                            found = true;
                        }
                    }
                }
            }

            //set the top left most open slot as occupied
            backBoard[rowToPlace][colToPlace] = 0;
            //updateShowBoard();
        }
    }//end makeMove1()

    //move for worker1
    public void makeMove2(){
        System.out.println("Worker 2 Turn: " + turn);
        turn++;
        boolean found = false;
        //if there are outstanding pieces remaining, add to the board from the bottom right most open spot
        if(piecesLeft2 > 0){
            for(int i = 2; i >= 0; i--){
                for(int j = 2; j >= 0; j--){
                    if(!found) {
                        if (backBoard[i][j] == -1) {
                            backBoard[i][j] = 1;
                            found = true;
                        }
                    }
                }
            }
            System.out.println("Pieces left: " + piecesLeft2);
            piecesLeft2--;
        }
        //finds the bottom right most open spot, and moves the top left most occupied slot to the open slot;
        else{
            int rowToPlace = -1, colToPlace = -1;

            //search for open
            for(int i = 2; i >= 0; i--){
                for(int j = 2; j >= 0; j--){
                    if(!found) {
                        if (backBoard[i][j] == -1) {
                            rowToPlace = i;
                            colToPlace = j;
                            found = true;
                        }
                    }
                }
            }

            found = false;
            //search for occupied until it is found, and set it to blank
            for(int i = 0; i < 3; i++){
                for(int j = 0; j < 3; j++){
                    if(!found) {
                        if (backBoard[i][j] == 1) {
                            backBoard[i][j] = -1;
                            found = true;
                        }
                    }
                }
            }

            //set the top left most open slot as occupied
            backBoard[rowToPlace][colToPlace] = 1;
        }
    }//end makeMove2()

    //worker1 thread
    private class Worker1Thread extends Thread{
        public Handler worker1Handler;
        private HandlerUI callbackHandler;

        public Worker1Thread(HandlerUI h){
            callbackHandler = h;
        }//end constructor

        public void run(){
            Looper.prepare();

            worker1Handler = new Handler(){
                @Override
                public void handleMessage(Message msg){
                    int what = msg.what ;
                    switch (what) {
                        case WAIT:
                            worker1Handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try { Thread.sleep(1000); }
                                    catch (InterruptedException e) { System.out.println("Thread interrupted!"); }

                                    //sending message back to UI handler
                                    Message message;
                                    message = callbackHandler.obtainMessage(DONE_WAITING);
                                    callbackHandler.sendMessage(message);
                                }
                            });

                        case MAKE_MOVE:
                            worker1Handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try { Thread.sleep(1000); }
                                    catch (InterruptedException e) { System.out.println("Thread interrupted!"); }

                                    makeMove1();

                                    //sending message back to UI handler
                                    Message message;
                                    message = callbackHandler.obtainMessage(UPDATE_BOARD);
                                    callbackHandler.sendMessage(message);
                                }
                            });
                            break;

                        case GAME_OVER:
                            worker1Handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println("Game over");
                                    try { Thread.sleep(1000); }
                                    catch (InterruptedException e) { System.out.println("Thread interrupted!"); }
                                    //stopThreads();
                                    //resetGame(0);
                                    gameOver = true;
                                    Toast.makeText(MainActivity.this, "Game Over", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                    }//end switch(...)
                }//end handleMessage()
            };//end handler

            Looper.loop();
        }//end run()
    }//end Worker1Thread class

    //worker2 thread
    private class Worker2Thread extends Thread{
        public Handler worker2Handler;
        private HandlerUI callbackHandler;

        public Worker2Thread(HandlerUI h){
            callbackHandler = h;
        }//end constructor

        public void run(){
            Looper.prepare();

            worker2Handler = new Handler(){
                @Override
                public void handleMessage(Message msg){
                    int what = msg.what ;
                    switch (what) {
                        case WAIT:
                            worker2Handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try { Thread.sleep(1000); }
                                    catch (InterruptedException e) { System.out.println("Thread interrupted!"); }

                                    //sending message back to UI handler
                                    Message message;
                                    message = callbackHandler.obtainMessage(DONE_WAITING);
                                    callbackHandler.sendMessage(message);
                                }
                            });

                        case MAKE_MOVE:
                            worker2Handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try { Thread.sleep(1000); }
                                    catch (InterruptedException e) { System.out.println("Thread interrupted!"); }

                                    makeMove2();

                                    //sending message back to UI handler
                                    Message message;
                                    message = callbackHandler.obtainMessage(UPDATE_BOARD);
                                    callbackHandler.sendMessage(message);
                                }
                            });
                            break;

                        case GAME_OVER:
                            worker2Handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println("Game over");
                                    try { Thread.sleep(1000); }
                                    catch (InterruptedException e) { System.out.println("Thread interrupted!"); }

                                }
                            });
                            break;
                    }//end switch(...)
                }//end handleMessage()
            };//end handler

            Looper.loop();
        }//end run()
    }//end Worker2Thread class


    //stops running the two worker threads
    public void stopThreads(){


        for(int i = 0; i < 5; i++){
            worker1Thread.worker1Handler.removeMessages(GAME_OVER);
            worker1Thread.worker1Handler.removeMessages(MAKE_MOVE);
            worker1Thread.worker1Handler.removeMessages(WAIT);

            worker2Thread.worker2Handler.removeMessages(GAME_OVER);
            worker2Thread.worker2Handler.removeMessages(MAKE_MOVE);
            worker2Thread.worker2Handler.removeMessages(WAIT);

            handlerUI.removeMessages(DONE_WAITING);
            handlerUI.removeMessages(NEXT_MOVE);
            handlerUI.removeMessages(UPDATE_BOARD);
        }

    }//end stopThreads()


}//end MainActivity class