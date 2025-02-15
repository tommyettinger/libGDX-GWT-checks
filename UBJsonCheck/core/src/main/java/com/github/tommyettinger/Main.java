package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;
    private BitmapFont font;
    private Json json;
    private RandomXS128 random;
    private String serialized  , ubSerialized   = "";
    private String deserialized, ubDeserialized = "";
    private ScreenViewport vp;

    public static void registerRandomXS128( Json json) {
        if(json.getSerializer(RandomXS128.class) != null) return;
        json.setSerializer(RandomXS128.class, new Json.Serializer<RandomXS128>() {
            @Override
            public void write(Json json, RandomXS128 object, Class knownType) {
                json.writeObjectStart();
                json.writeValue("state0", object.getState(0), long.class);
                json.writeValue("state1", object.getState(1), long.class);
                json.writeObjectEnd();
            }

            @Override
            public RandomXS128 read(Json json, JsonValue jsonData, Class type) {
                if (jsonData == null || jsonData.isNull()) return null;
                final long state0 = jsonData.getLong("state0");
                final long state1 = jsonData.getLong("state1");
                return new RandomXS128(state0, state1);
            }
        });
    }

    @Override
    public void create() {
        vp = new ScreenViewport();
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        font = new BitmapFont();
        random = new RandomXS128(0x1234567887654321L, 0x00000000FFFFFFFFL);
        json = new Json(JsonWriter.OutputType.json);
        registerRandomXS128(json);
        serialized   = json.prettyPrint(json.toJson(random, RandomXS128.class));
        deserialized = json.prettyPrint(json.fromJson(RandomXS128.class, serialized));

        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        UBJsonWriter ubWriter = new UBJsonWriter(baos);
        byte[] ba = new byte[0];
        try {
            ubWriter.value(new JsonReader().parse(serialized));
            ba = baos.toByteArray();
            ubDeserialized = "UBJson deserialized length="+ba.length;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        UBJsonReader ubReader = new UBJsonReader();
        ubReader.oldFormat = false;
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        JsonValue jv = ubReader.parse(bais);
        ubSerialized = json.prettyPrint(jv.prettyPrint(JsonWriter.OutputType.json, 120));

    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        vp.apply(true);
        batch.setProjectionMatrix(vp.getCamera().combined);
        batch.begin();
        batch.draw(image, 100, 440);
        font.draw(batch, "OK, let's see if this works..." +
            "\nShould have states " + 0x1234567887654321L + ", " + 0x00000000FFFFFFFFL +
            "\nSerialized:   " + serialized +
            "\nDeserialized: " + deserialized,
            100, 400, 400, Align.topLeft, true);
        font.draw(batch, "OK, let's see if this works..." +
            "\nShould have states " + 0x1234567887654321L + ", " + 0x00000000FFFFFFFFL +
            "\nUB Serialized:   " + ubSerialized +
            "\nUB Deserialized: " + ubDeserialized,
            500, 400, 400, Align.topLeft, true);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        vp.update(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        font.dispose();
    }
}
