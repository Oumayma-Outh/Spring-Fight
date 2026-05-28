package com.bgrant.sfs.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.bgrant.sfs.SFS;
import com.bgrant.sfs.objects.BloodPool;
import com.bgrant.sfs.objects.BloodSplatter;
import com.bgrant.sfs.objects.Fighter;
import com.bgrant.sfs.resources.Assets;
import com.bgrant.sfs.resources.GlobalVariables;

import java.util.Locale;

public abstract class GameScreen implements Screen, InputProcessor {
    private final SFS game;
    private final ExtendViewport viewport;

    // game
    private enum GameState { RUNNING, PAUSED, GAME_OVER }
    private GameState gameState;
    private GlobalVariables.Difficulty difficulty = GlobalVariables.Difficulty.EASY;

    // rounds
    private enum RoundState { STARTING, IN_PROGRESS, ENDING }
    private RoundState roundState;
    private float roundStateTime;
    private static final float START_ROUND_DELAY = 2f;
    private static final float END_ROUND_DELAY = 2f;
    private int currentRound;
    private static final int MAX_ROUNDS = 3;
    private int roundsWon = 0, roundsLost = 0;
    private static final float MAX_ROUND_TIME = 99.99f;
    private float roundTimer = MAX_ROUND_TIME;
    private static final float CRITICAL_ROUND_TIME = 10f;
    private static final Color CRITICAL_ROUND_TIME_COLOR = Color.RED;

    // fonts
    private BitmapFont smallFont, mediumFont, largeFont;
    private static final Color DEFAULT_FONT_COLOR = Color.WHITE;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // HUD
    private static final Color HEALTH_BAR_COLOR = Color.RED;
    private static final Color HEALTH_BAR_BACKGROUND_COLOR = GlobalVariables.GOLD;

    // background/ring
    private Texture backgroundTexture;
    private Texture frontRopesTexture;
    private static final float RING_MIN_X = 7f;
    private static final float RING_MAX_X = 60f;
    private static final float RING_MIN_Y = 4f;
    private static final float RING_MAX_Y = 22f;
    private static final float RING_SLOPE = 3.16f;

    // fighters
    private static final float PLAYER_START_POSITION_X = 16f;
    private static final float OPPONENT_START_POSITION_X = 51f;
    private static final float FIGHTER_START_POSITION_Y = 15f;
    private static final float FIGHTER_CONTACT_DISTANCE_X = 7.5f;
    private static final float FIGHTER_CONTACT_DISTANCE_Y = 1.5f;

    // buttons
    private Sprite playAgainButtonSprite;
    private Sprite mainMenuButtonSprite;
    private Sprite continueButtonSprite;
    private Sprite pauseButtonSprite;
    private static final float PAUSE_BUTTON_MARGIN = 1.5f;
    private static final float PAUSE_BUTTON_TOP_MARGIN = 8f;
    private static final float PAUSE_BUTTON_SIZE = 6f;

    // opponent AI
    private float opponentAiTimer;
    private boolean opponentAiMakingContactDecision;
    private static final float OPPONENT_AI_CONTACT_DECISION_DELAY_EASY   = 0.1f;
    private static final float OPPONENT_AI_CONTACT_DECISION_DELAY_MEDIUM  = 0.07f;
    private static final float OPPONENT_AI_CONTACT_DECISION_DELAY_HARD    = 0.01f;
    private static final float OPPONENT_AI_BLOCK_CHANCE                   = 0.4f;
    private static final float OPPONENT_AI_ATTACK_CHANCE                  = 0.8f;
    private static final float OPPONENT_AI_NON_CONTACT_DECISION_DELAY     = 0.5f;
    private boolean opponentAiPursuingPlayer;
    private static final float OPPONENT_AI_PURSUE_PLAYER_CHANCE_EASY   = 0.2f;
    private static final float OPPONENT_AI_PURSUE_PLAYER_CHANCE_MEDIUM = 0.5f;
    private static final float OPPONENT_AI_PURSUE_PLAYER_CHANCE_HARD   = 1f;

    // blood
    private boolean showingBlood = true;
    private BloodSplatter[] playerBloodSplatters;
    private BloodSplatter[] opponentBloodSplatters;
    private int currentPlayerBloodSplatterIndex;
    private int currentOpponentBloodSplatterIndex;
    private static final int BLOOD_SPLATTER_AMOUNT = 5;
    private static final float BLOOD_SPLATTER_OFFSET_X = 2.8f;
    private static final float BLOOD_SPLATTER_OFFSET_Y = 11f;
    private BloodPool[] bloodPools;
    private int currentBloodPoolIndex;
    private static final int BLOOD_POOL_AMOUNT = 100;

    // =========================================================
    // JOYSTICK VIRTUAL (izquierda inferior)
    // =========================================================
    private static final float JOYSTICK_BASE_X      = 6f;
    private static final float JOYSTICK_BASE_Y      = 7f;
    private static final float JOYSTICK_BASE_RADIUS = 4.5f;
    private static final float JOYSTICK_KNOB_RADIUS = 2f;
    private static final float JOYSTICK_DEAD_ZONE   = 0.5f;

    private int joystickPointer = -1;
    private final Vector2 joystickKnob = new Vector2();
    private boolean joystickMovingLeft  = false;
    private boolean joystickMovingRight = false;
    private boolean joystickMovingUp    = false;
    private boolean joystickMovingDown  = false;

    private static final Color JOYSTICK_BASE_COLOR = new Color(1f, 1f, 1f, 0.15f);
    private static final Color JOYSTICK_KNOB_COLOR = new Color(1f, 1f, 1f, 0.40f);

    // =========================================================
    // BOTONES DE ACCION (derecha inferior)
    // PUNCH = Puño   KICK = Patada   ELBOW = Codo
    // =========================================================
    private static final float ACTION_BUTTON_RADIUS = 3f;
    private static final float ACTION_BUTTON_MARGIN = 1.5f;

    private final Circle punchButtonCircle = new Circle();
    private final Circle kickButtonCircle  = new Circle();
    private final Circle blockButtonCircle = new Circle();

    private static final Color PUNCH_BUTTON_COLOR  = new Color(0.48f, 0.18f, 0.55f, 0.80f);
    private static final Color KICK_BUTTON_COLOR   = new Color(0.18f, 0.42f, 0.31f, 0.80f);
    private static final Color BLOCK_BUTTON_COLOR  = new Color(0.49f, 0.07f, 0.16f, 0.80f);
    private static final Color BUTTON_BORDER_COLOR = new Color(1f, 1f, 1f, 0.35f);

    // =========================================================

    public GameScreen(SFS game) {
        this.game = game;
        viewport = new ExtendViewport(GlobalVariables.WORLD_WIDTH, GlobalVariables.MIN_WORLD_HEIGHT,
            GlobalVariables.WORLD_WIDTH, 0);
        createGameArea();
        setUpFonts();
        createButtons();
        createBlood();
        joystickKnob.set(JOYSTICK_BASE_X, JOYSTICK_BASE_Y);
    }

    // ---------------------------------------------------------
    // Calcula posiciones de botones segun tamaño del mundo
    // ---------------------------------------------------------
    private void updateActionButtonPositions() {
        float w = viewport.getWorldWidth();
        float r = ACTION_BUTTON_RADIUS;
        float m = ACTION_BUTTON_MARGIN;
        punchButtonCircle.set(w - m - r,            m + r,            r);
        kickButtonCircle .set(w - m - r*3f - m,     m + r,            r);
        blockButtonCircle.set(w - m - r,             m + r*3f + m,     r);
    }

    // ---------------------------------------------------------
    // Posicion del boton PAUSE (arriba-derecha, pequeño y separado)
    // ---------------------------------------------------------
    private void positionPauseButton() {
        pauseButtonSprite.setPosition(
            viewport.getWorldWidth() - PAUSE_BUTTON_MARGIN - pauseButtonSprite.getWidth(),
            viewport.getWorldHeight() - PAUSE_BUTTON_TOP_MARGIN - pauseButtonSprite.getHeight()
        );
    }

    // ---------------------------------------------------------
    // Dibuja joystick y botones de accion
    // ---------------------------------------------------------
    private void renderTouchControls() {
        if (gameState != GameState.RUNNING) return;
        updateActionButtonPositions();

        game.batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // base joystick
        game.shapeRenderer.setColor(JOYSTICK_BASE_COLOR);
        game.shapeRenderer.circle(JOYSTICK_BASE_X, JOYSTICK_BASE_Y, JOYSTICK_BASE_RADIUS, 32);
        // nub joystick
        game.shapeRenderer.setColor(JOYSTICK_KNOB_COLOR);
        game.shapeRenderer.circle(joystickKnob.x, joystickKnob.y, JOYSTICK_KNOB_RADIUS, 32);

        // botones
        game.shapeRenderer.setColor(PUNCH_BUTTON_COLOR);
        game.shapeRenderer.circle(punchButtonCircle.x, punchButtonCircle.y, punchButtonCircle.radius, 32);
        game.shapeRenderer.setColor(KICK_BUTTON_COLOR);
        game.shapeRenderer.circle(kickButtonCircle.x, kickButtonCircle.y, kickButtonCircle.radius, 32);
        game.shapeRenderer.setColor(BLOCK_BUTTON_COLOR);
        game.shapeRenderer.circle(blockButtonCircle.x, blockButtonCircle.y, blockButtonCircle.radius, 32);

        game.shapeRenderer.end();

        // bordes
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        game.shapeRenderer.setColor(BUTTON_BORDER_COLOR);
        game.shapeRenderer.circle(JOYSTICK_BASE_X, JOYSTICK_BASE_Y, JOYSTICK_BASE_RADIUS, 32);
        game.shapeRenderer.circle(punchButtonCircle.x, punchButtonCircle.y, punchButtonCircle.radius, 32);
        game.shapeRenderer.circle(kickButtonCircle.x,  kickButtonCircle.y,  kickButtonCircle.radius,  32);
        game.shapeRenderer.circle(blockButtonCircle.x, blockButtonCircle.y, blockButtonCircle.radius, 32);
        game.shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        game.batch.begin();

        // etiquetas: PUNCH  KICK  ELBOW  (centradas en cada boton)
        glyphLayout.setText(smallFont, "PUNCH");
        smallFont.draw(game.batch, "PUNCH",
            punchButtonCircle.x - glyphLayout.width / 2f,
            punchButtonCircle.y + glyphLayout.height / 2f);

        glyphLayout.setText(smallFont, "KICK");
        smallFont.draw(game.batch, "KICK",
            kickButtonCircle.x - glyphLayout.width / 2f,
            kickButtonCircle.y + glyphLayout.height / 2f);

        glyphLayout.setText(smallFont, "ELBOW");
        smallFont.draw(game.batch, "ELBOW",
            blockButtonCircle.x - glyphLayout.width / 2f,
            blockButtonCircle.y + glyphLayout.height / 2f);
    }

    // ---------------------------------------------------------
    // Convierte pantalla -> mundo
    // ---------------------------------------------------------
    private Vector2 screenToWorld(int screenX, int screenY) {
        Vector3 v = new Vector3(screenX, screenY, 0);
        viewport.getCamera().unproject(v, viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight());
        return new Vector2(v.x, v.y);
    }

    // ---------------------------------------------------------
    // Touch down: joystick o boton de accion
    // ---------------------------------------------------------
    private boolean handleTouchDownControls(int screenX, int screenY, int pointer) {
        if (gameState != GameState.RUNNING) return false;
        Vector2 world = screenToWorld(screenX, screenY);

        // joystick
        if (joystickPointer == -1 &&
            Vector2.dst(world.x, world.y, JOYSTICK_BASE_X, JOYSTICK_BASE_Y) <= JOYSTICK_BASE_RADIUS) {
            joystickPointer = pointer;
            updateJoystick(world.x, world.y);
            return true;
        }

        updateActionButtonPositions();
        if (punchButtonCircle.contains(world)) { game.player.punch(); return true; }
        if (kickButtonCircle .contains(world)) { game.player.kick();  return true; }
        if (blockButtonCircle.contains(world)) { game.player.block(); return true; }

        return false;
    }

    // ---------------------------------------------------------
    // Touch dragged: mueve el nub del joystick
    // ---------------------------------------------------------
    private boolean handleTouchDraggedControls(int screenX, int screenY, int pointer) {
        if (pointer == joystickPointer) {
            Vector2 world = screenToWorld(screenX, screenY);
            updateJoystick(world.x, world.y);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------
    // Touch up: suelta joystick o bloqueo
    // ---------------------------------------------------------
    private boolean handleTouchUpControls(int screenX, int screenY, int pointer) {
        if (pointer == joystickPointer) {
            joystickPointer = -1;
            joystickKnob.set(JOYSTICK_BASE_X, JOYSTICK_BASE_Y);
            releaseJoystick();
            return true;
        }
        if (gameState != GameState.RUNNING) return false;
        Vector2 world = screenToWorld(screenX, screenY);
        updateActionButtonPositions();
        if (blockButtonCircle.contains(world)) { game.player.stopBlocking(); return true; }
        return false;
    }

    // ---------------------------------------------------------
    // Logica del joystick: mueve al jugador segun desplazamiento
    // ---------------------------------------------------------
    private void updateJoystick(float touchX, float touchY) {
        float dx   = touchX - JOYSTICK_BASE_X;
        float dy   = touchY - JOYSTICK_BASE_Y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > JOYSTICK_BASE_RADIUS) {
            dx = dx / dist * JOYSTICK_BASE_RADIUS;
            dy = dy / dist * JOYSTICK_BASE_RADIUS;
        }
        joystickKnob.set(JOYSTICK_BASE_X + dx, JOYSTICK_BASE_Y + dy);

        boolean wantLeft  = dx < -JOYSTICK_DEAD_ZONE;
        boolean wantRight = dx >  JOYSTICK_DEAD_ZONE;
        boolean wantUp    = dy >  JOYSTICK_DEAD_ZONE;
        boolean wantDown  = dy < -JOYSTICK_DEAD_ZONE;

        if (wantLeft  != joystickMovingLeft)  { joystickMovingLeft  = wantLeft;  if (wantLeft)  game.player.moveLeft();       else game.player.stopMovingLeft();  }
        if (wantRight != joystickMovingRight) { joystickMovingRight = wantRight; if (wantRight) game.player.moveRight();      else game.player.stopMovingRight(); }
        if (wantUp    != joystickMovingUp)    { joystickMovingUp    = wantUp;    if (wantUp)    game.player.moveUp();         else game.player.stopMovingUp();    }
        if (wantDown  != joystickMovingDown)  { joystickMovingDown  = wantDown;  if (wantDown)  game.player.moveDown();       else game.player.stopMovingDown();  }
    }

    private void releaseJoystick() {
        if (joystickMovingLeft)  { game.player.stopMovingLeft();  joystickMovingLeft  = false; }
        if (joystickMovingRight) { game.player.stopMovingRight(); joystickMovingRight = false; }
        if (joystickMovingUp)    { game.player.stopMovingUp();    joystickMovingUp    = false; }
        if (joystickMovingDown)  { game.player.stopMovingDown();  joystickMovingDown  = false; }
    }

    // =========================================================

    private void createGameArea() {
        backgroundTexture = game.assets.manager.get(Assets.BACKGROUND_TEXTURE);
        frontRopesTexture = game.assets.manager.get(Assets.FRONT_ROPES_TEXTURE);
    }

    private void setUpFonts() {
        smallFont = game.assets.manager.get(Assets.SMALL_FONT);
        smallFont.getData().setScale(GlobalVariables.WORLD_SCALE);
        smallFont.setColor(DEFAULT_FONT_COLOR);
        smallFont.setUseIntegerPositions(false);

        mediumFont = game.assets.manager.get(Assets.MEDIUM_FONT);
        mediumFont.getData().setScale(GlobalVariables.WORLD_SCALE);
        mediumFont.setColor(DEFAULT_FONT_COLOR);
        mediumFont.setUseIntegerPositions(false);

        largeFont = game.assets.manager.get(Assets.LARGE_FONT);
        largeFont.getData().setScale(GlobalVariables.WORLD_SCALE);
        largeFont.setColor(DEFAULT_FONT_COLOR);
        largeFont.setUseIntegerPositions(false);
    }

    private void createButtons() {
        TextureAtlas buttonTextureAtlas = game.assets.manager.get(Assets.GAMEPLAY_BUTTONS_ATLAS);

        playAgainButtonSprite = new Sprite(buttonTextureAtlas.findRegion("PlayAgainButton"));
        playAgainButtonSprite.setSize(playAgainButtonSprite.getWidth() * GlobalVariables.WORLD_SCALE,
            playAgainButtonSprite.getHeight() * GlobalVariables.WORLD_SCALE);

        mainMenuButtonSprite = new Sprite(buttonTextureAtlas.findRegion("MainMenuButton"));
        mainMenuButtonSprite.setSize(mainMenuButtonSprite.getWidth() * GlobalVariables.WORLD_SCALE,
            mainMenuButtonSprite.getHeight() * GlobalVariables.WORLD_SCALE);

        continueButtonSprite = new Sprite(buttonTextureAtlas.findRegion("ContinueButton"));
        continueButtonSprite.setSize(continueButtonSprite.getWidth() * GlobalVariables.WORLD_SCALE,
            continueButtonSprite.getHeight() * GlobalVariables.WORLD_SCALE);

        pauseButtonSprite = new Sprite(buttonTextureAtlas.findRegion("PauseButton"));
        // Tamaño pequeño igual a los botones de acción (6x6)
        pauseButtonSprite.setSize(PAUSE_BUTTON_SIZE, PAUSE_BUTTON_SIZE);
    }

    private void createBlood() {
        playerBloodSplatters   = new BloodSplatter[BLOOD_SPLATTER_AMOUNT];
        opponentBloodSplatters = new BloodSplatter[BLOOD_SPLATTER_AMOUNT];
        for (int i = 0; i < BLOOD_SPLATTER_AMOUNT; i++) {
            playerBloodSplatters[i]   = new BloodSplatter(game);
            opponentBloodSplatters[i] = new BloodSplatter(game);
        }
        currentPlayerBloodSplatterIndex   = 0;
        currentOpponentBloodSplatterIndex = 0;

        bloodPools = new BloodPool[BLOOD_POOL_AMOUNT];
        for (int i = 0; i < BLOOD_POOL_AMOUNT; i++) bloodPools[i] = new BloodPool(game);
        currentBloodPoolIndex = 0;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this);
        difficulty   = game.settingsManager.getDifficultySetting();
        showingBlood = game.settingsManager.isBloodSettingOn();
        startGame();
    }

    private void startGame() {
        gameState  = GameState.RUNNING;
        roundsWon  = roundsLost = 0;
        currentRound = 1;
        startRound();
    }

    private void pauseGame() {
        gameState = GameState.PAUSED;
        game.audioManager.pauseGameSounds();
        game.audioManager.pauseMusic();
    }

    private void resumeGame() {
        gameState = GameState.RUNNING;
        game.audioManager.resumeGameSounds();
        game.audioManager.playMusic();
    }

    private void startRound() {
        game.player.getReady(PLAYER_START_POSITION_X, FIGHTER_START_POSITION_Y);
        game.opponent.getReady(OPPONENT_START_POSITION_X, FIGHTER_START_POSITION_Y);
        roundState     = RoundState.STARTING;
        roundStateTime = 0f;
        roundTimer     = MAX_ROUND_TIME;
        // resetear joystick
        joystickPointer = -1;
        joystickKnob.set(JOYSTICK_BASE_X, JOYSTICK_BASE_Y);
        releaseJoystick();
    }

    private void endRound()  { roundState = RoundState.ENDING;  roundStateTime = 0f; }

    private void winRound() {
        game.player.win(); game.opponent.lose(); roundsWon++;
        game.audioManager.playSound(Assets.CHEER_SOUND);
        endRound();
    }

    private void loseRound() {
        game.player.lose(); game.opponent.win(); roundsLost++;
        game.audioManager.playSound(Assets.BOO_SOUND);
        endRound();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        update(gameState == GameState.RUNNING ? delta : 0f);

        game.batch.setProjectionMatrix(viewport.getCamera().combined);
        game.shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);

        game.batch.begin();
        game.batch.draw(backgroundTexture, 0, 0,
            backgroundTexture.getWidth() * GlobalVariables.WORLD_SCALE,
            backgroundTexture.getHeight() * GlobalVariables.WORLD_SCALE);
        renderBloodPools();
        renderFighters();
        game.batch.draw(frontRopesTexture, 0, 0,
            frontRopesTexture.getWidth() * GlobalVariables.WORLD_SCALE,
            frontRopesTexture.getHeight() * GlobalVariables.WORLD_SCALE);
        renderHUD();
        renderPauseButton();
        renderTouchControls();

        if (gameState == GameState.GAME_OVER) {
            renderGameOverOverlay();
        } else {
            if (roundState == RoundState.STARTING) renderStartRoundText();
            if (gameState  == GameState.PAUSED)    renderPauseOverlay();
        }
        game.batch.end();
    }

    private void renderFighters() {
        if (game.player.getPosition().y > game.opponent.getPosition().y) {
            game.player.render(game.batch);   renderBloodSplatters(playerBloodSplatters);
            game.opponent.render(game.batch); renderBloodSplatters(opponentBloodSplatters);
        } else {
            game.opponent.render(game.batch); renderBloodSplatters(opponentBloodSplatters);
            game.player.render(game.batch);   renderBloodSplatters(playerBloodSplatters);
        }
    }

    private void renderBloodSplatters(BloodSplatter[] bs) {
        if (showingBlood) for (BloodSplatter b : bs) b.render(game.batch);
    }

    private void renderBloodPools() {
        if (showingBlood) for (BloodPool bp : bloodPools) bp.render(game.batch);
    }

    private void renderHUD() {
        float m = 1f;
        smallFont.draw(game.batch, "WINS: " + roundsWon + " - " + roundsLost, m, viewport.getWorldHeight() - m);

        String text = "DIFFICULTY: ";
        switch (difficulty) { case EASY: text+="EASY"; break; case MEDIUM: text+="MEDIUM"; break; default: text+="HARD"; }
        smallFont.draw(game.batch, text, viewport.getWorldWidth()-m, viewport.getWorldHeight()-m, 0, Align.right, false);

        float hp=0.5f, hh=smallFont.getCapHeight()+hp*2f, hmw=32f, hbp=0.2f;
        float hbh=hh+hbp*2f, hbw=hmw+hbp*2f, hbmt=0.8f;
        float hbpy=viewport.getWorldHeight()-m-smallFont.getCapHeight()-hbmt-hbh;
        float hpy=hbpy+hbp, fny=hpy+hh-hp;

        game.batch.end();
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(HEALTH_BAR_BACKGROUND_COLOR);
        game.shapeRenderer.rect(m, hbpy, hbw, hbh);
        game.shapeRenderer.rect(viewport.getWorldWidth()-m-hbw, hbpy, hbw, hbh);
        game.shapeRenderer.setColor(HEALTH_BAR_COLOR);
        float hw = hmw * game.player.getLife() / Fighter.MAX_LIFE;
        game.shapeRenderer.rect(m+hbp, hpy, hw, hh);
        hw = hmw * game.opponent.getLife() / Fighter.MAX_LIFE;
        game.shapeRenderer.rect(viewport.getWorldWidth()-m-hbp-hw, hpy, hw, hh);
        game.shapeRenderer.end();
        game.batch.begin();

        smallFont.draw(game.batch, game.player.getName(), m+hbp+hp, fny);
        smallFont.draw(game.batch, game.opponent.getName(), viewport.getWorldWidth()-m-hbp-hp, fny, 0, Align.right, false);

        if (roundTimer < CRITICAL_ROUND_TIME) mediumFont.setColor(CRITICAL_ROUND_TIME_COLOR);
        mediumFont.draw(game.batch, String.format(Locale.getDefault(), "%02d", (int) roundTimer),
            viewport.getWorldWidth()/2f - mediumFont.getSpaceXadvance()*2.3f, viewport.getWorldHeight()-m);
        mediumFont.setColor(DEFAULT_FONT_COLOR);
    }

    private void renderStartRoundText() {
        String text = (roundStateTime < START_ROUND_DELAY*0.5f) ? "ROUND "+currentRound : "FIGHT!";
        mediumFont.draw(game.batch, text, viewport.getWorldWidth()/2f, viewport.getWorldHeight()/2f, 0, Align.center, false);
    }

    private void renderPauseButton() {
        positionPauseButton();
        pauseButtonSprite.draw(game.batch);
    }

    private void renderGameOverOverlay() {
        game.batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(0,0,0,0.7f);
        game.shapeRenderer.rect(0,0,viewport.getWorldWidth(),viewport.getWorldHeight());
        game.shapeRenderer.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
        game.batch.begin();

        float tb=2f, bs=0.5f;
        float lh=largeFont.getCapHeight()+tb+playAgainButtonSprite.getHeight()+bs+mainMenuButtonSprite.getHeight();
        float ly=viewport.getWorldHeight()/2f-lh/2f;
        mainMenuButtonSprite.setPosition(viewport.getWorldWidth()/2f-mainMenuButtonSprite.getWidth()/2f, ly);
        mainMenuButtonSprite.draw(game.batch);
        playAgainButtonSprite.setPosition(viewport.getWorldWidth()/2f-playAgainButtonSprite.getWidth()/2f, ly+mainMenuButtonSprite.getHeight()+bs);
        playAgainButtonSprite.draw(game.batch);
        String text = (roundsWon > roundsLost) ? "YOU WON!" : "YOU LOST!";
        largeFont.draw(game.batch, text, viewport.getWorldWidth()/2f,
            playAgainButtonSprite.getY()+playAgainButtonSprite.getHeight()+tb+largeFont.getCapHeight(), 0, Align.center, false);
    }

    private void renderPauseOverlay() {
        game.batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.shapeRenderer.setColor(0,0,0,0.7f);
        game.shapeRenderer.rect(0,0,viewport.getWorldWidth(),viewport.getWorldHeight());
        game.shapeRenderer.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
        game.batch.begin();

        float tb=2f, bs=0.5f;
        float lh=largeFont.getCapHeight()+tb+continueButtonSprite.getHeight()+bs+mainMenuButtonSprite.getHeight();
        float ly=viewport.getWorldHeight()/2f-lh/2f;
        mainMenuButtonSprite.setPosition(viewport.getWorldWidth()/2f-mainMenuButtonSprite.getWidth()/2f, ly);
        mainMenuButtonSprite.draw(game.batch);
        continueButtonSprite.setPosition(viewport.getWorldWidth()/2f-continueButtonSprite.getWidth()/2f, ly+mainMenuButtonSprite.getHeight()+bs);
        continueButtonSprite.draw(game.batch);
        largeFont.draw(game.batch, "GAME PAUSED", viewport.getWorldWidth()/2f,
            continueButtonSprite.getY()+continueButtonSprite.getHeight()+tb+largeFont.getCapHeight(), 0, Align.center, false);
    }

    private void update(float deltaTime) {
        if (roundState == RoundState.STARTING && roundStateTime >= START_ROUND_DELAY) {
            roundState = RoundState.IN_PROGRESS; roundStateTime = 0f;
        } else if (roundState == RoundState.ENDING && roundStateTime >= END_ROUND_DELAY) {
            if (roundsWon > MAX_ROUNDS/2 || roundsLost > MAX_ROUNDS/2) gameState = GameState.GAME_OVER;
            else { currentRound++; startRound(); }
        } else {
            roundStateTime += deltaTime;
        }

        game.player.update(deltaTime);
        game.opponent.update(deltaTime);
        for (int i = 0; i < BLOOD_SPLATTER_AMOUNT; i++) {
            playerBloodSplatters[i].update(deltaTime);
            opponentBloodSplatters[i].update(deltaTime);
        }
        for (BloodPool bp : bloodPools) bp.update(deltaTime);

        if (game.player.getPosition().x <= game.opponent.getPosition().x) { game.player.faceRight(); game.opponent.faceLeft(); }
        else { game.player.faceLeft(); game.opponent.faceRight(); }

        keepWithinRingBounds(game.player.getPosition());
        keepWithinRingBounds(game.opponent.getPosition());

        if (roundState == RoundState.IN_PROGRESS) {
            roundTimer -= deltaTime;
            if (roundTimer <= 0f) { if (game.player.getLife() >= game.opponent.getLife()) winRound(); else loseRound(); }

            performOpponentAi(deltaTime);

            if (areWithinContactDistance(game.player.getPosition(), game.opponent.getPosition())) {
                if (game.player.isAttackActive()) {
                    game.opponent.getHit(Fighter.HIT_STRENGTH);
                    if (game.opponent.isBlocking()) game.audioManager.playSound(Assets.BLOCK_SOUND);
                    else { game.audioManager.playSound(Assets.HIT_SOUND); spillBlood(game.opponent); }
                    game.player.makeContact();
                    if (game.opponent.hasLost()) winRound();
                } else if (game.opponent.isAttackActive()) {
                    game.player.getHit(Fighter.HIT_STRENGTH);
                    if (game.player.isBlocking()) game.audioManager.playSound(Assets.BLOCK_SOUND);
                    else { game.audioManager.playSound(Assets.HIT_SOUND); spillBlood(game.player); }
                    game.opponent.makeContact();
                    if (game.player.hasLost()) loseRound();
                }
            }
        }
    }

    private void spillBlood(Fighter fighter) {
        BloodSplatter[] bs;
        int idx;
        if (fighter.equals(game.player)) { bs = playerBloodSplatters;   idx = currentPlayerBloodSplatterIndex; }
        else                             { bs = opponentBloodSplatters; idx = currentOpponentBloodSplatterIndex; }
        bs[idx].activate(fighter.getPosition().x + BLOOD_SPLATTER_OFFSET_X, fighter.getPosition().y + BLOOD_SPLATTER_OFFSET_Y);
        if (fighter.equals(game.player))
            currentPlayerBloodSplatterIndex   = (currentPlayerBloodSplatterIndex   < BLOOD_SPLATTER_AMOUNT-1) ? currentPlayerBloodSplatterIndex+1   : 0;
        else
            currentOpponentBloodSplatterIndex = (currentOpponentBloodSplatterIndex < BLOOD_SPLATTER_AMOUNT-1) ? currentOpponentBloodSplatterIndex+1 : 0;
        bloodPools[currentBloodPoolIndex].activate(fighter.getPosition().x, fighter.getPosition().y);
        currentBloodPoolIndex = (currentBloodPoolIndex < BLOOD_POOL_AMOUNT-1) ? currentBloodPoolIndex+1 : 0;
    }

    private void keepWithinRingBounds(Vector2 p) {
        if (p.y < RING_MIN_Y) p.y = RING_MIN_Y; else if (p.y > RING_MAX_Y) p.y = RING_MAX_Y;
        if (p.x < p.y/RING_SLOPE+RING_MIN_X)  p.x = p.y/RING_SLOPE+RING_MIN_X;
        else if (p.x > p.y/-RING_SLOPE+RING_MAX_X) p.x = p.y/-RING_SLOPE+RING_MAX_X;
    }

    private boolean areWithinContactDistance(Vector2 p1, Vector2 p2) {
        return Math.abs(p1.x-p2.x) <= FIGHTER_CONTACT_DISTANCE_X && Math.abs(p1.y-p2.y) <= FIGHTER_CONTACT_DISTANCE_Y;
    }

    private void performOpponentAi(float deltaTime) {
        if (opponentAiMakingContactDecision) {
            if (game.opponent.isBlocking()) {
                if (!areWithinContactDistance(game.player.getPosition(), game.opponent.getPosition()) ||
                    !game.player.isAttacking() || game.player.hasMadeContact())
                    game.opponent.stopBlocking();
            } else if (!game.opponent.isAttacking()) {
                if (areWithinContactDistance(game.player.getPosition(), game.opponent.getPosition())) {
                    if (opponentAiTimer <= 0f) opponentAiMakeContactDecision(); else opponentAiTimer -= deltaTime;
                } else { opponentAiMakingContactDecision = false; }
            }
        } else {
            if (areWithinContactDistance(game.player.getPosition(), game.opponent.getPosition())) {
                opponentAiMakeContactDecision();
            } else {
                if (opponentAiTimer <= 0f) {
                    float pc = difficulty==GlobalVariables.Difficulty.EASY ? OPPONENT_AI_PURSUE_PLAYER_CHANCE_EASY
                        : difficulty==GlobalVariables.Difficulty.MEDIUM ? OPPONENT_AI_PURSUE_PLAYER_CHANCE_MEDIUM
                        : OPPONENT_AI_PURSUE_PLAYER_CHANCE_HARD;
                    if (MathUtils.random() <= pc) { opponentAiPursuingPlayer=true;  opponentAiMoveTowardPlayer(); }
                    else                          { opponentAiPursuingPlayer=false; opponentAiMoveRandomly(); }
                    opponentAiTimer = OPPONENT_AI_NON_CONTACT_DECISION_DELAY;
                } else {
                    if (opponentAiPursuingPlayer) opponentAiMoveTowardPlayer();
                    opponentAiTimer -= deltaTime;
                }
            }
        }
    }

    private void opponentAiMakeContactDecision() {
        opponentAiMakingContactDecision = true;
        if (game.player.isAttacking()) {
            if (!game.player.hasMadeContact()) {
                if (MathUtils.random() <= OPPONENT_AI_BLOCK_CHANCE) game.opponent.block();
                else opponentAiMoveAwayFromPlayer();
            }
        } else {
            if (MathUtils.random() <= OPPONENT_AI_ATTACK_CHANCE) {
                if (MathUtils.random(1)==0) game.opponent.punch(); else game.opponent.kick();
            } else opponentAiMoveAwayFromPlayer();
        }
        switch(difficulty) {
            case EASY:   opponentAiTimer=OPPONENT_AI_CONTACT_DECISION_DELAY_EASY;   break;
            case MEDIUM: opponentAiTimer=OPPONENT_AI_CONTACT_DECISION_DELAY_MEDIUM; break;
            default:     opponentAiTimer=OPPONENT_AI_CONTACT_DECISION_DELAY_HARD;
        }
    }

    private void opponentAiMoveTowardPlayer() {
        Vector2 pp=game.player.getPosition(), op=game.opponent.getPosition();
        if (op.x > pp.x+FIGHTER_CONTACT_DISTANCE_X)      game.opponent.moveLeft();
        else if (op.x < pp.x-FIGHTER_CONTACT_DISTANCE_X) game.opponent.moveRight();
        else { game.opponent.stopMovingLeft(); game.opponent.stopMovingRight(); }
        if (op.y < pp.y-FIGHTER_CONTACT_DISTANCE_Y)      game.opponent.moveUp();
        else if (op.y > pp.y+FIGHTER_CONTACT_DISTANCE_Y) game.opponent.moveDown();
        else { game.opponent.stopMovingUp(); game.opponent.stopMovingDown(); }
    }

    private void opponentAiMoveRandomly() {
        switch(MathUtils.random(2)) { case 0: game.opponent.moveLeft(); break; case 1: game.opponent.moveRight(); break; default: game.opponent.stopMovingLeft(); game.opponent.stopMovingRight(); }
        switch(MathUtils.random(2)) { case 0: game.opponent.moveUp();   break; case 1: game.opponent.moveDown();  break; default: game.opponent.stopMovingUp();   game.opponent.stopMovingDown();  }
    }

    private void opponentAiMoveAwayFromPlayer() {
        Vector2 pp=game.player.getPosition(), op=game.opponent.getPosition();
        if (op.x > pp.x) game.opponent.moveRight(); else game.opponent.moveLeft();
        if (op.y > pp.y) game.opponent.moveUp();    else game.opponent.moveDown();
    }

    @Override public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override public void pause()   { if (gameState==GameState.RUNNING) pauseGame(); game.audioManager.pauseMusic(); }
    @Override public void resume()  { game.audioManager.playMusic(); }
    @Override public void hide()    { }
    @Override public void dispose() { }

    // =========================================================
    // INPUT - Teclado
    // =========================================================
    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.SPACE) {
            if (gameState==GameState.RUNNING) {
                if (roundState==RoundState.STARTING)     roundStateTime=START_ROUND_DELAY;
                else if (roundState==RoundState.ENDING)  roundStateTime=END_ROUND_DELAY;
            } else if (gameState==GameState.GAME_OVER) startGame();
            else resumeGame();
        } else if ((gameState==GameState.RUNNING||gameState==GameState.PAUSED) && keycode==Input.Keys.P) {
            if (gameState==GameState.RUNNING) pauseGame(); else resumeGame();
        } else if (keycode==Input.Keys.M) { game.audioManager.toggleMusic();
        } else if (keycode==Input.Keys.L) {
            switch(difficulty) { case EASY: difficulty=GlobalVariables.Difficulty.MEDIUM; break; case MEDIUM: difficulty=GlobalVariables.Difficulty.HARD; break; default: difficulty=GlobalVariables.Difficulty.EASY; }
        } else if (keycode==Input.Keys.K) { showingBlood=!showingBlood;
        } else if (gameState==GameState.RUNNING) {
            if (keycode==Input.Keys.LEFT||keycode==Input.Keys.A)  game.player.moveLeft();
            else if (keycode==Input.Keys.RIGHT||keycode==Input.Keys.D) game.player.moveRight();
            if (keycode==Input.Keys.UP||keycode==Input.Keys.W)    game.player.moveUp();
            else if (keycode==Input.Keys.DOWN||keycode==Input.Keys.S)  game.player.moveDown();
            if (keycode==Input.Keys.B)      game.player.block();
            else if (keycode==Input.Keys.F) game.player.punch();
            else if (keycode==Input.Keys.V) game.player.kick();
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode==Input.Keys.LEFT||keycode==Input.Keys.A)  game.player.stopMovingLeft();
        else if (keycode==Input.Keys.RIGHT||keycode==Input.Keys.D) game.player.stopMovingRight();
        if (keycode==Input.Keys.UP||keycode==Input.Keys.W)    game.player.stopMovingUp();
        else if (keycode==Input.Keys.DOWN||keycode==Input.Keys.S)  game.player.stopMovingDown();
        if (keycode==Input.Keys.B) game.player.stopBlocking();
        return true;
    }

    @Override public boolean keyTyped(char c) { return false; }

    // =========================================================
    // INPUT - Tactil
    // =========================================================
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Primero intentamos joystick / botones de accion
        if (handleTouchDownControls(screenX, screenY, pointer)) return true;

        // Luego botones de menu
        Vector3 pos = new Vector3(screenX, screenY, 0);
        viewport.getCamera().unproject(pos, viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight());

        // Asegurar posicion correcta del pause button antes del hit-test
        positionPauseButton();

        if (gameState==GameState.RUNNING) {
            if (pauseButtonSprite.getBoundingRectangle().contains(pos.x, pos.y)) { pauseGame(); game.audioManager.playSound(Assets.CLICK_SOUND); }
            else if (roundState==RoundState.STARTING) roundStateTime=START_ROUND_DELAY;
            else if (roundState==RoundState.ENDING)   roundStateTime=END_ROUND_DELAY;
        } else {
            if (gameState==GameState.GAME_OVER && playAgainButtonSprite.getBoundingRectangle().contains(pos.x,pos.y)) {
                startGame(); game.audioManager.playSound(Assets.CLICK_SOUND);
            } else if (gameState==GameState.PAUSED && continueButtonSprite.getBoundingRectangle().contains(pos.x,pos.y)) {
                resumeGame(); game.audioManager.playSound(Assets.CLICK_SOUND);
            } else if (mainMenuButtonSprite.getBoundingRectangle().contains(pos.x,pos.y)) {
                game.audioManager.playSound(Assets.CLICK_SOUND);
                game.audioManager.stopGameSounds();
                if (gameState==GameState.PAUSED) game.audioManager.playMusic();
                for (int i=0;i<BLOOD_SPLATTER_AMOUNT;i++) { playerBloodSplatters[i].deactivate(); opponentBloodSplatters[i].deactivate(); }
                game.setScreen(game.mainMenuScreen);
            }
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        handleTouchUpControls(screenX, screenY, pointer);
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        handleTouchDraggedControls(screenX, screenY, pointer);
        return false;
    }

    @Override public boolean mouseMoved(int screenX, int screenY)   { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }
}
