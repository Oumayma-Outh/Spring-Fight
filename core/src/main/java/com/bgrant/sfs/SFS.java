package com.bgrant.sfs;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.bgrant.sfs.objects.Fighter;
import com.bgrant.sfs.objects.FighterChoice;
import com.bgrant.sfs.resources.Assets;
import com.bgrant.sfs.resources.AudioManager;
import com.bgrant.sfs.resources.SettingsManager;
import com.bgrant.sfs.screens.GameScreen;
import com.bgrant.sfs.screens.LoadingScreen;
import com.bgrant.sfs.screens.MainMenuScreen;
import com.bgrant.sfs.screens.SettingsScreen;

import java.util.ArrayList;

public class SFS extends Game {
    public SpriteBatch batch;
    public ShapeRenderer shapeRenderer;
    public Assets assets;
    public AudioManager audioManager;
    public SettingsManager settingsManager;

    // screens
    public GameScreen gameScreen;
    public MainMenuScreen mainMenuScreen;
    public SettingsScreen settingsScreen;
    public LoadingScreen loadingScreen;

    // fighters
    public Fighter player, opponent;
    public final ArrayList<FighterChoice> fighterChoiceList = new ArrayList<>();

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        assets = new Assets();

        // initialize the loading screen and switch to it
        loadingScreen = new LoadingScreen(this);
        setScreen(loadingScreen);
    }

    public void assetsLoaded() {
        // initialize the settings manager and load all the settings
        settingsManager = new SettingsManager();
        settingsManager.loadSettings();

        // initialize the audio manager
        audioManager = new AudioManager(assets.manager);

        // update the audio settings in the audio manager
        if (settingsManager.isMusicSettingOn()) {
            audioManager.enableMusic();
        } else {
            audioManager.disableMusic();
        }
        if (settingsManager.isSoundsSettingOn()) {
            audioManager.enableSounds();
        } else {
            audioManager.disableSounds();
        }

        // if the full screen setting is on, go to full screen
        if (settingsManager.isFullScreenSettingOn()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }

        // load the fighter choice list
        loadFighterChoiceList();

        // initialize the fighters
        player = new Fighter(this, fighterChoiceList.get(0).getName(), fighterChoiceList.get(0).getColor());
        opponent = new Fighter(this, fighterChoiceList.get(1).getName(), fighterChoiceList.get(1).getColor());

        // initialize the game screen
        gameScreen = new GameScreen(this) {
            @Override
            public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
                return false;
            }
        };

        // initialize the settings screen
        settingsScreen = new SettingsScreen(this);

        // initialize the main menu screen and switch to it
        mainMenuScreen = new MainMenuScreen(this);
        setScreen(mainMenuScreen);
    }

    private void loadFighterChoiceList() {
        // load the fighter choice list from assets
        Json json = new Json();
        JsonValue fighterChoices = new JsonReader().parse(Gdx.files.internal("data/fighter_choices.json"));
        for (int i = 0; i < fighterChoices.size; i++) {
            fighterChoiceList.add(json.fromJson(FighterChoice.class, String.valueOf(fighterChoices.get(i))));
        }
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        assets.dispose();
    }
}
