package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.compression.Lzma;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.io.BufferedInputStream;
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
    private String serialized  , ubSerialized   = "", ubLzmaSerialized   = "", lzmaSerialized   = "";
    private String deserialized, ubDeserialized = "", ubLzmaDeserialized = "", lzmaDeserialized = "";
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

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
            UBJsonWriter ubWriter = new UBJsonWriter(baos);
            ubWriter.value(new JsonReader().parse(serialized));
            byte[] ba = baos.toByteArray();
            ubSerialized = "UBJson serialized length=" + ba.length;

            UBJsonReader ubReader = new UBJsonReader();
            ubReader.oldFormat = false;
            BufferedInputStream bais = new BufferedInputStream(new ByteArrayInputStream(ba));
            JsonValue jv = ubReader.parse(bais);
            ubDeserialized = json.prettyPrint(jv.prettyPrint(JsonWriter.OutputType.json, 120));

            baos.reset();
            bais = new BufferedInputStream(new ByteArrayInputStream(ba));
            Lzma.compress(bais, baos);
            byte[] ba2 = baos.toByteArray();
            ubLzmaSerialized = "UBJson+LZMA serialized length=" + ba2.length;
            bais = new BufferedInputStream(new ByteArrayInputStream(ba2));
            baos.reset();
            Lzma.decompress(bais, baos);
            ba2 = baos.toByteArray();
            bais = new BufferedInputStream(new ByteArrayInputStream(ba2));
            jv = ubReader.parse(bais);
            ubLzmaDeserialized = json.prettyPrint(jv.prettyPrint(JsonWriter.OutputType.json, 120));

            baos.reset();
            bais = new BufferedInputStream(new ByteArrayInputStream(serialized.getBytes("UTF-8")));
            Lzma.compress(bais, baos);
            ba2 = baos.toByteArray();
            lzmaSerialized = "Json+LZMA serialized length=" + ba2.length;
            bais = new BufferedInputStream(new ByteArrayInputStream(ba2));
            baos.reset();
            Lzma.decompress(bais, baos);
            JsonReader jr = new JsonReader();
            jv = jr.parse(baos.toString("UTF-8"));
            lzmaDeserialized = json.prettyPrint(jv.prettyPrint(JsonWriter.OutputType.json, 120));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        vp.apply(true);
        batch.setProjectionMatrix(vp.getCamera().combined);
        batch.begin();
//        batch.draw(image, 100, 440);
        font.draw(batch, "OK, let's see if Json works..." +
            "\nShould have states " + 0x1234567887654321L + ", " + 0x00000000FFFFFFFFL +
            "\nSerialized:   " + serialized +
            "\nDeserialized: " + deserialized,
            50, 600, 300, Align.topLeft, true);
//        font.draw(batch, "OK, let's see if UBJson works..." +
//            "\nShould have states " + 0x1234567887654321L + ", " + 0x00000000FFFFFFFFL +
//            "\nUB Serialized:   " + ubSerialized +
//            "\nUB Deserialized: " + ubDeserialized,
//            375, 600, 300, Align.topLeft, true);
        font.draw(batch, "OK, let's see if Json+LZMA works..." +
            "\nShould have states " + 0x1234567887654321L + ", " + 0x00000000FFFFFFFFL +
            "\nLZMA Serialized:   " + lzmaSerialized +
            "\nLZMA Deserialized: " + lzmaDeserialized,
            375, 600, 300, Align.topLeft, true);
        font.draw(batch, "OK, let's see if UBJson+LZMA works..." +
            "\nShould have states " + 0x1234567887654321L + ", " + 0x00000000FFFFFFFFL +
            "\nUB+LZMA Serialized:   " + ubLzmaSerialized +
            "\nUB+LZMA Deserialized: " + ubLzmaDeserialized,
            700, 600, 300, Align.topLeft, true);
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
