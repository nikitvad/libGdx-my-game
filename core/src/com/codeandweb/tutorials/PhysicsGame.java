package com.codeandweb.tutorials;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.codeandweb.physicseditor.PhysicsShapeCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PhysicsGame extends ApplicationAdapter {

    static final float STEP_TIME = 1f / 60f;

    static final int VELOCITY_ITERATIONS = 6;

    static final int POSITION_ITERATIONS = 2;

    static final float SCALE = 0.05f;


    static final int COUNT = 10;
    static final String TAG = "TOUCH";
    private static final String OBJECT_TYPE_BULLET = "banana_bullet";

    final HashMap<String, Sprite> sprites = new HashMap<String, Sprite>();

    TextureAtlas textureAtlas;
    SpriteBatch batch;
    OrthographicCamera camera;
    ExtendViewport viewport;
    World world;
    PhysicsShapeCache physicsBodies;
    float accumulator = 0;
    Body ground;

    ArrayList<Body> fruitBodies = new ArrayList<Body>();
    ArrayList<Sprite> fruitSprites = new ArrayList<Sprite>();

    Vector2 gravity = new Vector2(0, -4);
    Sprite fireZoneSprite;
    Circle fireZone;
    ArrayList<Body> objectsToRemove = new ArrayList<Body>();
    Thread thread;
    private Body ship;
    private float screenWidth = 100;
    private float screenHeight = 100;
    private float prevX = 0;
    private float prevY = 0;
    private Circle touchZone = new Circle(0, 0, 5);
    private int firePointer = -1;
    private int motionPointer = -1;
    private List<Body> bananaBullets = new ArrayList<Body>();
    private String OBJECT_TYPE_FRUIT = "fruit";

    @Override
    public void create() {
        Box2D.init();

        batch = new SpriteBatch();

        camera = new OrthographicCamera();

        viewport = new ExtendViewport(50, 50, camera);

        textureAtlas = new TextureAtlas("sprites.txt");

        loadSprites();
        createFireZone();

        world = new World(new Vector2(0, -10), true);

        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Body bodyA = contact.getFixtureA().getBody();
                Body bodyB = contact.getFixtureB().getBody();

                if (bodyA == ship || bodyB == ship) {

                    if (bodyB == ship) {
                        Body temp = bodyA;
                        bodyA = bodyB;
                        bodyB = temp;
                    }
                    Vector2 impulse = bodyA.getWorldCenter();
                    impulse.x = (bodyB.getWorldCenter().x - impulse.x) * 100;
                    impulse.y = (bodyB.getWorldCenter().y - impulse.y) * 100;
                    Gdx.app.log("asdf", impulse.toString());


                    bodyB.applyLinearImpulse(impulse, bodyB.getWorldCenter(), true);
                } else if (bodyA.getUserData() != null && bodyB.getUserData() != null && !bodyA.getUserData().equals(bodyB.getUserData())) {

                    if (bodyA.getUserData().equals(OBJECT_TYPE_BULLET)) {
                        removeBullets(bodyA);
                    } else if (bodyA.getUserData().equals(OBJECT_TYPE_FRUIT)) {
                        removeFruit(bodyA);
                    }

                    if (bodyB.getUserData().equals(OBJECT_TYPE_BULLET)) {
                        removeBullets(bodyB);
                    } else if (bodyB.getUserData().equals(OBJECT_TYPE_FRUIT)) {
                        removeFruit(bodyB);
                    }

                }
            }

            @Override
            public void endContact(Contact contact) {

            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {

            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {

            }
        });

        physicsBodies = new PhysicsShapeCache("physics.xml");


        createShip();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {

                Vector3 touchPos = new Vector3(screenX, screenY, 0);
                Vector3 worldPos = camera.unproject(touchPos);

                touchZone.x = worldPos.x;
                touchZone.y = worldPos.y;

//                Gdx.app.log(TAG, "touchDown " + fireZone.overlaps(touchZone));

                if (fireZone.overlaps(touchZone)) {
                    firePointer = pointer;
                    fireTrigger();
                } else {
                    if (motionPointer < 0) {
                        motionPointer = pointer;
                    }
                    prevX = screenX;
                    prevY = screenY;
                }
                return super.touchDown(screenX, screenY, pointer, button);
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (pointer == firePointer) {

                    firePointer = -1;
                }

                if (pointer == motionPointer) {
                    motionPointer = -1;
                }

                Gdx.app.log(TAG, "touchUp " + screenX + " " + screenY + " " + pointer + " " + button);
                return super.touchUp(screenX, screenY, pointer, button);

            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {

                if (pointer == motionPointer) {
                    float dX = (screenX - prevX);
                    float dY = (screenY - prevY);

                    Vector2 vector2 = ship.getPosition();

                    float newXPos = vector2.x + (dX / screenWidth) * camera.viewportWidth;
                    float newYPos = vector2.y + (-dY / screenHeight) * camera.viewportHeight;

                    if (newXPos > camera.viewportWidth) {
                        newXPos = camera.viewportWidth;
                    }

                    if (newXPos < 0) {
                        newXPos = 0;
                    }

                    if (newYPos > camera.viewportHeight) {
                        newYPos = camera.viewportHeight;
                    }

                    if (newYPos < 0) {
                        newYPos = 0;
                    }

                    Gdx.app.log(TAG, "touchDragged " + newXPos + " " + newXPos + " " + pointer);

                    vector2.x = newXPos;
                    vector2.y = newYPos;

                    prevY = screenY;
                    prevX = screenX;


                    ship.setTransform(vector2, 0);

                    Gdx.app.log(TAG, "touchDragged " + screenX + " " + screenY + " " + pointer);
                }
                return super.touchDragged(screenX, screenY, pointer);
            }
        });

    }

    private void createFireZone() {
        fireZoneSprite = textureAtlas.createSprite("orange");

        float width = fireZoneSprite.getWidth() * SCALE * 2;
        float height = fireZoneSprite.getHeight() * SCALE * 2;
        fireZoneSprite.setOrigin(0, 0);
        fireZoneSprite.setAlpha(0.6f);
        fireZoneSprite.setSize(width, height);

        fireZone = new Circle();
        fireZone.setRadius(width / 2);
        fireZone.setPosition(0, 0);

    }

    private void createShip() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;


        ship = physicsBodies.createBody("cherries", world, bodyDef, SCALE, SCALE);
        ship.setTransform(20, 20, 0);
    }

    /**
     * Loads the sprites and caches them into {@link #sprites}.
     */
    private void loadSprites() {
        Array<AtlasRegion> regions = textureAtlas.getRegions();

        for (AtlasRegion region : regions) {
            Sprite sprite = textureAtlas.createSprite(region.name);

            float width = sprite.getWidth() * SCALE;
            float height = sprite.getHeight() * SCALE;

            sprite.setSize(width, height);
            sprite.setOrigin(0, 0);

            sprites.put(region.name, sprite);
        }
    }

    private void generateFruit() {
        final String[] fruitNames = new String[]{"orange"};

        final Random random = new Random();

        for (int i = 0; i < COUNT; i++) {
            String name = fruitNames[random.nextInt(fruitNames.length)];

            fruitSprites.add(i, sprites.get(name));

            float x = random.nextFloat() * 50;
            float y = camera.viewportHeight - 10;

            Body body = createBody(name, x, y, 0);
            body.setUserData(OBJECT_TYPE_FRUIT);

            fruitBodies.add(i, body);
        }

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.interrupted()) {
                        Gdx.app.log("sdfsdf", "thread is working");

                        if (fruitBodies.size() < 10 && !world.isLocked()) {
                            Gdx.app.log("sdfsdf", "dsfsd");
                            String name = fruitNames[random.nextInt(fruitNames.length)];

                            fruitSprites.add(fruitSprites.size(), sprites.get(name));
                            float x = random.nextFloat() * 50;
                            float y = camera.viewportHeight;

                            Body body = createBody(name, x, y, 0);
                            body.setUserData(OBJECT_TYPE_FRUIT);

                            fruitBodies.add(fruitBodies.size(), body);


                        }
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    private Body createBody(String name, float x, float y, float rotation) {
        Body body = physicsBodies.createBody(name, world, SCALE, SCALE);
        body.setTransform(x, y, rotation);

        return body;
    }

    /**
     * This is called when the application is resized, and can happen at any
     * time, but will never be called before {@link #create()}.
     *
     * @param width  The screen's new width in pixels.
     * @param height The screen's new height in pixels.
     */
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);

        generateFruit();


        screenWidth = width;
        screenHeight = height;

        batch.setProjectionMatrix(camera.combined);

        fireZone.setPosition(camera.viewportWidth - fireZone.radius * 2, 0);
        fireZoneSprite.setPosition(fireZone.x, fireZone.y);

        createGround();
    }

    private void createGround() {
        if (ground != null) world.destroyBody(ground);

        BodyDef bodyDef = new BodyDef();

        bodyDef.type = BodyDef.BodyType.StaticBody;

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.friction = 1;

        PolygonShape bottom = new PolygonShape();
        bottom.setAsBox(camera.viewportWidth, 1);

        PolygonShape left = new PolygonShape();
        left.setAsBox(1, camera.viewportHeight + 10);

        Vector2[] rightEdge = new Vector2[]{new Vector2(camera.viewportWidth, 0),
                new Vector2(camera.viewportWidth, camera.viewportHeight + 10),
                new Vector2(camera.viewportWidth - 1, camera.viewportHeight + 10),
                new Vector2(camera.viewportWidth - 1, 0)};
        PolygonShape right = new PolygonShape();
        right.set(rightEdge);


        Vector2[] topEdge = new Vector2[]{new Vector2(0, camera.viewportHeight + 10),
                new Vector2(camera.viewportWidth, camera.viewportHeight + 10),
                new Vector2(0, camera.viewportHeight + 9),
                new Vector2(camera.viewportWidth, camera.viewportHeight + 9)};
        PolygonShape top = new PolygonShape();
        top.set(topEdge);

        fixtureDef.shape = bottom;

        ground = world.createBody(bodyDef);
        ground.createFixture(fixtureDef);
//        ground.setTransform(0, 0, 0);

        fixtureDef.shape = left;

        ground = world.createBody(bodyDef);
        ground.createFixture(fixtureDef);
//        ground.setTransform(0, 0, 0);


        fixtureDef.shape = right;

        ground = world.createBody(bodyDef);
        ground.createFixture(fixtureDef);
//        ground.setTransform(0, 0, 0);

        fixtureDef.shape = top;

        ground = world.createBody(bodyDef);
        ground.createFixture(fixtureDef);

        bottom.dispose();
        left.dispose();
        right.dispose();
    }

    @Override
    public void render() {
        // Clear the screen using a sky-blue background.
        Gdx.gl.glClearColor(0.57f, 0.77f, 0.85f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        // Step the physics world.
        stepWorld();

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            Vector2 vector2 = ship.getPosition();

            vector2.x -= 1;
            ship.setTransform(vector2, 0);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            Vector2 vector2 = ship.getPosition();

            vector2.x += 1;
            ship.setTransform(vector2, 0);
        }


        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            Vector2 vector2 = ship.getPosition();

            vector2.y += 1;
            ship.setTransform(vector2, 0);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            Vector2 vector2 = ship.getPosition();

            vector2.y -= 1;
            ship.setTransform(vector2, 0);
        }

        batch.begin();


        drawSprite(sprites.get("cherries"), ship.getPosition().x, ship.getPosition().y, 0);
        // iterate through each of the fruits
        for (int i = 0; i < fruitBodies.size(); i++) {

            Body body = fruitBodies.get(i);

            Vector2 position = body.getPosition();

            float degrees = (float) Math.toDegrees(body.getAngle());

            drawSprite(fruitSprites.get(i), position.x, position.y, degrees);
        }

        for (int i = 0; i < bananaBullets.size(); i++) {
            Body bullet = bananaBullets.get(i);

            Vector2 p = bullet.getPosition();

            if (p.y >= camera.viewportHeight) {
                removeBullets(bullet);
                i--;
                continue;
            }

            float degrees = (float) Math.toDegrees(bullet.getAngle());
            drawSprite(sprites.get("banana"), p.x, p.y, degrees);
        }

        fireZoneSprite.draw(batch);
        batch.end();

        for (Body item : objectsToRemove) {
            world.destroyBody(item);
        }
        objectsToRemove.clear();

    }

    private void stepWorld() {
        float delta = Gdx.graphics.getDeltaTime();

        accumulator += Math.min(delta, 0.25f);

        if (accumulator >= STEP_TIME) {
            accumulator -= STEP_TIME;

            world.step(STEP_TIME, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
        }
    }

    private void drawSprite(Sprite sprite, float x, float y, float degrees) {
        sprite.setPosition(x, y);

        sprite.setRotation(degrees);

        sprite.draw(batch);
    }

    private void fireTrigger() {
        if (!world.isLocked()) {
            Vector2 pos = ship.getWorldCenter();
            Body body = createBody("banana", pos.x, pos.y + 10, 0);
            body.applyLinearImpulse(0, 1000, body.getWorldCenter().x, body.getWorldCenter().y, true);
            body.setGravityScale(0);
            body.setUserData(OBJECT_TYPE_BULLET);
            bananaBullets.add(body);
        }

    }

    @Override
    public void dispose() {
        textureAtlas.dispose();
        sprites.clear();
        if (thread != null && !thread.isInterrupted()) {
            thread.interrupt();
        }

        world.dispose();
    }

    private void removeBullets(Body body) {
        bananaBullets.remove(body);
        body.setUserData(null);
        objectsToRemove.add(body);
    }

    private void removeFruit(Body body) {
        int pos = fruitBodies.indexOf(body);
        if (pos >= 0) {

            fruitBodies.remove(pos);
            fruitSprites.remove(pos);
            body.setUserData(null);
            objectsToRemove.add(body);

        }
    }


}

