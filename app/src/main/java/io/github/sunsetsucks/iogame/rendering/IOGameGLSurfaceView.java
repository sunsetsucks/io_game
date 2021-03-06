package io.github.sunsetsucks.iogame.rendering;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import io.github.sunsetsucks.iogame.Util;
import io.github.sunsetsucks.iogame.rendering.player.Chaser;
import io.github.sunsetsucks.iogame.rendering.player.Player;
import io.github.sunsetsucks.iogame.rendering.player.Runner;
import io.github.sunsetsucks.iogame.rendering.powerup.GrowthPowerup;
import io.github.sunsetsucks.iogame.rendering.powerup.InvincibilityPowerup;
import io.github.sunsetsucks.iogame.rendering.powerup.Powerup;
import io.github.sunsetsucks.iogame.rendering.powerup.SpeedDownPowerup;
import io.github.sunsetsucks.iogame.rendering.powerup.SpeedUpPowerup;

/**
 * Created by ssuri on 7/25/16.
 */
public class IOGameGLSurfaceView extends GLSurfaceView
{
    public Renderer renderer;

    public IOGameGLSurfaceView(Context context)
    {
        super(context);
        init();
    }

    public IOGameGLSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    private void init()
    {
        setEGLContextClientVersion(2);

        setPreserveEGLContextOnPause(true);

        renderer = new Renderer();

        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    private float targetX = 0, targetY = 0;

    private MotionEvent lastEvent;

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e)
    {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN
                || e.getActionMasked() == MotionEvent.ACTION_MOVE)
        {

            float xScreen = e.getX();
            float yScreen = e.getY();

            WindowManager wm = (WindowManager) Util.context
                    .getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int screenWidth = size.x, screenHeight = size.y;

            targetX = (xScreen / screenWidth) * -2.0f + 1.0f + renderer.cameraX;
            targetY = (yScreen / screenHeight) * -2.0f + 1.0f
                    + renderer.cameraY;

            targetX = Util.clamp(targetX, -15, 14.5f);
            targetY = Util.clamp(targetY, -15.5f, 13.6f);

            lastEvent = e;
        }

        return true;
    }

    public void udpUpdate(byte[] bytes)
    {
        Player p;

        byte compId = Player.compId(bytes);

        if(compId == Util.compId) return;

        if (renderer.players.containsKey(compId))
        {
            p = renderer.players.get(compId);
        } else if (!renderer.deadPlayers.contains(compId))
        {
            if (Player.isChaser(bytes))
            {
                p = new Chaser(compId);
            } else
            {
                p = new Runner(compId);
            }

            renderer.players.put(compId, p);
        } else
            return;

        p.fromBytes(bytes);
    }

    public static int loadShader(int type, String shaderCode)
    {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public class Renderer implements GLSurfaceView.Renderer
    {
        public List<GameObject> toDraw = new ArrayList<>();
        public List<Powerup> powerups = new ArrayList<>();
        public Map<Byte, Player> players = new HashMap<>();
        public List<Byte> deadPlayers = new ArrayList<>();

        private float cameraX = 0f, cameraY = 0f;

        // model view projection
        private final float[] mvpMatrix = new float[16],
                projectionMatrix = new float[16], viewMatrix = new float[16];

        public Powerup generateObject()
        {
            Random r = new Random();
            float x, y;
            x = (float) Math.random() * 14;
            if (r.nextBoolean())
                x = x * -1.0f;
            y = (float) Math.random() * 14;
            if (r.nextBoolean())
                y = y * -1.0f;

            Powerup p;
            int rand = r.nextInt(100);
            if (rand < 25) // 25 percent
            {
                p = new SpeedUpPowerup();
            } else if (rand < 50) // 25 percent
            {
                p = new InvincibilityPowerup();
            } else if (rand < 75) // 25 percent
            {
                p = new SpeedDownPowerup();
            } else // 25 percent
            {
                p = new GrowthPowerup();
            }

            p.translationX = x;
            p.translationY = y;

            Util.broadcastMessage(p.toSerializable(false), true);

            return p;
        }

        public boolean checkCollision(GameObject a, GameObject b)
        {
            boolean aCollision = a.translationX + a.scaleX >= b.translationX
                    && b.translationX + b.scaleX >= a.translationX;
            boolean bCollision = a.translationY + a.scaleY >= b.translationY
                    && b.translationY + b.scaleY >= a.translationY;
            return aCollision && bCollision;
        }

        public void onSurfaceCreated(GL10 unused, EGLConfig config)
        {
            float[] color = Color.GLAUCOUS;
            GLES20.glClearColor(color[0], color[1], color[2], color[3]);

            toDraw.add(new Square(Util.loadBitmap("drawable/grid", 2))
                    .setState(0f, 0f, -1f, 30f, 30f));
        }

        private boolean startGame = false;

        public void startGame()
        {
            startGame = true;
        }

        private void _startGame()
        {
            Player p;

            if (Util.isHost)
            {
                p = new Chaser(Util.compId);
                for (int i = 0; i < 16; i++)
                {
                    powerups.add(generateObject());
                }
            } else
            {
                p = new Runner(Util.compId);
            }

            Random r = new Random();
            float x, y;
            x = (float) Math.random() * 14;
            if (r.nextBoolean())
                x = x * -1.0f;
            y = (float) Math.random() * 14;
            if (r.nextBoolean())
                y = y * -1.0f;

            p.translationX = x;
            p.translationY = y;
            targetX = x;
            targetY = y;

            players.put(Util.compId, p);

            startGame = false;
        }

        public void playerDied(byte b)
        {
            players.remove(b);
            deadPlayers.add(b);

            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    ((Activity) Util.context).finish();
                }
            };

            DialogInterface.OnDismissListener listener2 = new DialogInterface.OnDismissListener()
            {
                @Override
                public void onDismiss(DialogInterface dialogInterface)
                {
                    ((Activity) Util.context).finish();
                }
            };

            if (b == Util.compId)
            {
                Util.alert("rekt", "u ded", listener, listener2);
            }

            if (Util.compId == 0 && players.size() == 1)
            {
                Util.alert("Good job!", "Do you feel good about yourself? You just murdered all the runners.", listener, listener2);
            }
        }

        private int powerUp = 0;
        public void powerUp(boolean isSpeedUp)
        {
            powerUp = isSpeedUp ? 1 : 2;
        }

        long lastTime = -1;

        public void onDrawFrame(GL10 unused)
        {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            if (startGame)
            {
                _startGame();
            }


            Player player = players.get(Util.compId);

            switch(powerUp)
            {
                case 1:
                    player.speedChange();
                    powerUp = 0;
                    break;
                case 2:
                    player.powerUp();
                    powerUp = 0;
                    break;
            }

            long thisTime = System.nanoTime();
            if (lastTime != -1 && player != null)
            {
                float playerX = player.translationX;
                float playerY = player.translationY;

                float totalDistance = (float) Math.hypot(targetX - playerX,
                        targetY - playerY);

                if (totalDistance != 0)
                {
                    float distanceToTravel = Math.min(totalDistance,
                            player.getSpeed()
                                    * ((thisTime - lastTime) / 1_000_000_000f));

                    float ratio = distanceToTravel / totalDistance;

                    playerX = (playerX + (ratio * (targetX - playerX)));
                    playerY = (playerY + (ratio * (targetY - playerY)));

                    playerX = Util.clamp(playerX, -15, 14.5f);
                    playerY = Util.clamp(playerY, -15.5f, 13.6f);

                } else if (lastEvent != null)
                {
                    onTouchEvent(lastEvent);
                }

                cameraX = player.translationX = playerX;
                cameraY = player.translationY = playerY;

                Util.broadcastMessage(player.toBytes(), false);
            }
            lastTime = thisTime;

            Matrix.setLookAtM(viewMatrix, 0, /* eye */ cameraX, cameraY, -6f,
                    /* center */ cameraX, cameraY, 0f, /* up */ 0f, 1f, 0f);

            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

            // Checks collisions and notifies the relevant host

            if (Util.isHost)
            {
                for (Byte b : players.keySet())
                {
                    List<Powerup> toRemove = new ArrayList<>(), toAdd = new ArrayList<>();
                    for (Powerup p : powerups)
                    {
                        if (checkCollision(p, players.get(b))
                                && p.doesContact(players.get(b)))
                        {
                            toRemove.add(p);
                            toAdd.add(generateObject());

                            if(b == 0)
                            {
                                if (p.isSpeedRelated())
                                {
                                    players.get(b).speedChange();
                                }
                                else
                                {
                                    players.get(b).powerUp();
                                }
                            }
                            else
                            {
                                HashMap<String, Object> toSend = new HashMap<>();
                                toSend.put("type", "powerupCollided");
                                toSend.put("isSpeedRelated", p.isSpeedRelated());
                                Util.sendMessage(toSend, true, b);
                            }

                            Serializable message = p.toSerializable(true);
                            Util.broadcastMessage(message, true);
                        }
                    }
                    powerups.removeAll(toRemove);
                    powerups.addAll(toAdd);
                }

                List<Byte> toRemove = new ArrayList<>();
                for (byte b : players.keySet())
                {
                    if (b == 0)
                    {
                        continue;
                    }

                    Player p = players.get(b);

                    if (p instanceof Runner && !p.isPoweredUp() && checkCollision(player, p))
                    {
                        Util.broadcastMessage(p.destroyedMessage(), true);
                        toRemove.add(b);
                    }
                }

                for (Byte b : toRemove)
                {
                    playerDied(b);
                }
            }

            // Draw all three kinds of game objects
            for (GameObject go : toDraw)
            {
//                System.out.println("Drawing generic game obj");
                go.draw(mvpMatrix);
            }

            for (Powerup pow : powerups)
            {
//                System.out.println("Drawing power up");
                pow.draw(mvpMatrix);
            }
            for (Player p : players.values())
            {
//                System.out.println("Drawing player");

                if(p instanceof Chaser)
                {
                    if(p.isPoweredUp())
                    {
                        p.scaleX = p.scaleY = 2f;
                    }
                    else
                    {
                        p.scaleX = p.scaleY = 1f;
                    }
                }
                else if(p instanceof Runner)
                {
                    if(p.isPoweredUp())
                    {
                        p.currentTexture = 1;
                    }
                    else
                    {
                        p.currentTexture = 0;
                    }
                }

                p.draw(mvpMatrix);
            }
        }

        public void onSurfaceChanged(GL10 unused, int width, int height)
        {
            GLES20.glViewport(0, 0, width, height);

            float ratio = (float) width / height;

            // apply the projection matrix
            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        }
    }
}