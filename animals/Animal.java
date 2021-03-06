package animals;

import diet.IDiet;
import food.EFoodType;
import food.IEdible;
import graphics.*;
import mobility.Mobile;
import mobility.Point;
import utilities.MessageUtility;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * A class that contains all fields of an animal object,
 * including GUI components and implements runnable.
 * 
 * @version 1.3 09 June 2022
 * @author Ido Ben Nun, Bar Cohen
 * @see Observable
 */
public abstract class Animal extends Observable implements IEdible ,IDrawable, IAnimalBehavior, Runnable {

	private static final int X_DIR_RIGHT = 1, X_DIR_LEFT = -1, Y_DIR_UP = 1, Y_DIR_DOWN = -1, MIN_SIZE = 50, MAX_SIZE = 300, SPEED = 75;
	private final int EAT_DISTANCE = 10;
	private int size;
	private String col;
	private int horSpeed;
	private int verSpeed;
	private boolean coordChanged = false;
	private int x_dir; //
	private int y_dir;
	private int eatCount;
	private ZooPanel pan;
	protected BufferedImage img1 = null, img2 = null;
	private String name;
	private double weight;
	private IDiet diet;
	private Point location;
	protected boolean threadSuspended = false;
	private boolean exit = false;
	private Vector<Observer> list = new Vector<>();

	/**
	 * This function is used to register
	 * the observer to subject.
	 * @param ob (Observer) to be registered.
	 */
	public void registerObserver(Observer ob) {list.add(ob);}

	/**
	 * This function is used to unregister
	 * the observer from subject.
	 * @param ob (Observer) to be unregistered.
	 */
	public synchronized void unregisterObserver(Observer ob) {
		int index = list.indexOf(ob);
		list.set(index, list.lastElement());
		list.remove(list.size()-1);
	}

	/**
	 * Notify all of its observers
	 * if the object has changed.
	 * @param msg
	 */
	public void notifyObservers(String msg) {
		for (Observer ob: list)
			ob.notify(getAnimalName() + msg);
	}

	/**
	 * We use this function to set a memento object,
	 * in order to restore state of this object.
	 * @param memento (Memento) object to restore state.
	 */
	public void setMemento(Memento memento)
	{
		setSize(memento.getSize());
		setWeight(memento.getWeight());
		setHorSpeed(memento.getHorSpeed());
		setVerSpeed(memento.getVerSpeed());
		setColor(memento.getColor());
		setX_dir(memento.getX_dir());
		setY_dir(memento.getY_dir());
		setLocation(memento.getLocation());
		setEatCount(memento.getEatCount());
		loadImages(memento.getColor()); // Change color of animal
		notifyObservers(" is restored");
	}

	/**
	 * When an object implementing interface Runnable is used to create a thread,
	 * starting the thread causes the object's run method to be called in that separately executing thread.
	 * In this run() method we change the location of an animal object and perform related actions.
	 */
	@Override
	public void run() {
		while (!exit) {
			try {
				while (diet.canEat(getPan().getFood().getFoodType())) {
					if (location.getX() >= getPan().getFood().getLocation().getX()) {
						this.x_dir = X_DIR_LEFT;
						if (location.getY() >= getPan().getFood().getLocation().getY())
							location=new Point(location.getX() - horSpeed, location.getY() - verSpeed);
					}
					if (location.getX() <= getPan().getFood().getLocation().getX()) {
						this.x_dir = X_DIR_RIGHT;
						if (location.getY() <= getPan().getFood().getLocation().getY())
							location=new Point(location.getX() + horSpeed, location.getY() + verSpeed);
					}
					if (location.getX() <= getPan().getFood().getLocation().getX()) {
						this.x_dir = X_DIR_RIGHT;
						if (location.getY() >= getPan().getFood().getLocation().getY())
							location=new Point(location.getX() + horSpeed, location.getY() - verSpeed);
					}
					if (location.getX() >= getPan().getFood().getLocation().getX()) {
						this.x_dir = X_DIR_LEFT;
						if (location.getY() <= getPan().getFood().getLocation().getY())
							location=new Point(location.getX() - horSpeed, location.getY() + verSpeed);
					}
					getPan().repaint();
					getPan().manageZoo();
					try {
						Thread.sleep(SPEED);
					} catch (InterruptedException ignored) {}
				}
			} catch (NullPointerException ignored) {}

			if (location.getX() >= getPan().getWidth() || location.getX() <= 0) {
				if (location.getX() == 0) location.setX(1);
				if (x_dir == X_DIR_RIGHT) setX_dir(X_DIR_LEFT);
				else setX_dir(X_DIR_RIGHT);
			}
			if (location.getY() >= getPan().getHeight() || location.getY() <= 0) {
				if (location.getY() == 0) location.setY(1);
				if (y_dir == Y_DIR_UP) setY_dir(Y_DIR_DOWN);
				else setY_dir(Y_DIR_UP);
			}
			location=new Point(location.getX() + horSpeed * x_dir, location.getY() + verSpeed * y_dir);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					getPan().repaint();
					getPan().manageZoo();
				}
			});
			try {
				Thread.sleep(SPEED);
			} catch (InterruptedException ignored) {}
			while (threadSuspended) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace(); }
				}
			}
		}
	}

	/**
	 * A Ctor of animal to be used with graphics package.
	 * @param size (Integer) Size of animal on the panel.
	 * @param horSpeed (Integer) Horizontal speed.
	 * @param verSpeed (Integer) Vertical speed.
	 * @param color (String) Color of animal image.
	 * @param weight (Double) Weight of animal.
	 */
	public Animal(int size, int horSpeed, int verSpeed, String color, double weight) {
		setSize(size);
		setWeight(weight);
		setHorSpeed(horSpeed);
		setVerSpeed(verSpeed);
		setColor(color);
		setX_dir(1); // Default X direction
		setY_dir(1); // Default	Y direction
	}

	/**
	 * A simple synchronized method,
	 * Allows us to put the animal thread in a waiting position.
	 */
	public synchronized void setSuspended() { this.threadSuspended = true;
	notifyObservers(" is asleep now");}

	/**
	 * A simple synchronized method,
	 * Allows us to put the animal thread back in a running position.
	 */
	public synchronized void setResumed() {
		this.exit = false;
		this.threadSuspended = false;
		notify();
		notifyObservers(" is awake and hungry");}

	/**
	 * A simple synchronized method,
	 * Allows us to terminate the animal thread.
	 */
	public synchronized void stop() {
		exit = true;
		notifyObservers(" is no longer exists");
	}

	/**
	 * A simple getter of color to file.
	 * @param color
	 * 			(String) Color.
	 * @return (String) part-of-string of the file name to be loaded.
	 */
	public String getColorToFile(String color) {
		return switch (color) {
			case "Natural" -> "n";
			case "Blue" -> "b";
			case "Red" -> "r";
			default -> null;
		};
	}

	/**
	 * A getter of color string.
	 * @return (String) color.
	 */
	public String getColorToString() { return col; }

	/**
	 * A getter of color.
	 * @return (Color) color.
	 */
	public Color getColor() {
		return switch (col) {
			case "Red" -> Color.RED;
			case "Blue" -> Color.BLUE;
			default -> null;	// case "Natural" -> null;
		};
	}

	/**
	 * A setter of horSpeed.
	 * @param horSpeed Horizontal speed (Integer).
	 * @return True if succeeded, otherwise false.
	 */
	public boolean setHorSpeed(int horSpeed) {
		if (horSpeed < 0 || horSpeed > 10) {
			MessageUtility.logSetter(getClass().getSimpleName(), "setHorSpeed", horSpeed, false);
			return false; }
		else this.horSpeed = horSpeed;
		MessageUtility.logSetter(getClass().getSimpleName(), "setHorSpeed", horSpeed, true);
		return true; }

	/**
	 * A setter of verSpeed.
	 * @param verSpeed Horizontal speed (Integer).
	 * @return True if succeeded, otherwise false.
	 */
	public boolean setVerSpeed(int verSpeed) {
		if (verSpeed < 0 || verSpeed > 10) {
			MessageUtility.logSetter(getClass().getSimpleName(), "setVerSpeed", verSpeed, false);
			return false; }
		else this.verSpeed = verSpeed;
		MessageUtility.logSetter(getClass().getSimpleName(), "setVerSpeed", verSpeed, true);
		return true; }

	/**
	 * A setter of x_dir.
	 * @param x_dir Y Direction (Integer).
	 * @return True if succeeded, otherwise false.
	 */
	public boolean setX_dir(int x_dir) {
		if (x_dir != X_DIR_LEFT && x_dir != X_DIR_RIGHT) {
			return false;
		}
		else this.x_dir = x_dir;
		return true; }

	/**
	 * A setter of y_dir.
	 * @param y_dir Y Direction (Integer).
	 * @return True if succeeded, otherwise false.
	 */
	public boolean setY_dir(int y_dir) {
		if (y_dir != Y_DIR_DOWN && y_dir != Y_DIR_UP) {
			return false;
		}
		else this.y_dir = y_dir;
		return true; }

	/**
	 * A setter of animal size.
	 * @param size
	 * 			size of animal on the panel (Integer).
	 * @return True if succeeded, otherwise false.
	 */
	public boolean setSize(int size) {
		if (size < MIN_SIZE || size > MAX_SIZE) return false;
		else this.size = size;
		return true; }

	/**
	 * A getter of animal name.
	 * Not relevant after HW1.
	 * @return (String) Name of animal.
	 */
	public String getAnimalName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * A setter of animal color.
	 * @param col
	 * 			Color of animal to use in panel.
	 * @return True if succeeded, otherwise false.
	 */
	public boolean setColor(String col) {
		if (col.equals("Natural") || col.equals("Blue") || col.equals("Red")) {
			this.col = col;
			return true; }
		return false;
	}

	/**
	 * A setter of the panel to be used with GUI components.
	 * @param pan - (ZooPanel) Panel for drawing animals.
	 * @return (Boolean) true if applied, otherwise false.
	 */
	public boolean setPan(ZooPanel pan) {
		if (pan == null)
			return false;
		else this.pan = pan;
		return true;
	}

	/**
	 * A simple function to draw an animal on the panel.
	 * @param g
	 * 			Graphics of pan.
	 */
	public void drawObject (Graphics g)
	{
		if(getX_dir()==1) // animal goes to the right side
			g.drawImage(img1, location.getX()-size/2, location.getY()-size/10, size/2, size, pan);
		else // animal goes to the left side
			g.drawImage(img2, location.getX(), location.getY()-size/10, size/2, size, pan);
	}

	/**
	 * A simple function to increase eatCount.
	 */
	public void eatInc() { this.eatCount++; }

	/**
	 * A getter of eatCount.
	 * @return eatCount.
	 */
	public int getEatCount() { return this.eatCount; }

	/**
	 * A getter of horSpeed.
	 * @return (Integer) horSpeed.
	 */
	public int getHorSpeed() { return this.horSpeed; }

	/**
	 * A getter of verSpeed.
	 * @return (Integer) verSpeed.
	 */
	public int getVerSpeed() { return this.verSpeed; }

	/**
	 * A getter of coordChanged.
	 * @return (Boolean) value of coordChanged.
	 */
	public boolean getChanges() { return this.coordChanged; }

	/**
	 * A setter of coordChanged.
	 * @param coordChanged
	 * 			(Boolean) value.
	 */
	public void setChanges(boolean coordChanged) {
		if (coordChanged)
			setX_dir(1);
		else setX_dir(-1);
	}

	/**
	 * A getter of X coordinate.
	 * @return (Integer) X coordinate.
	 */
	public int getX_dir() { return x_dir; }

	/**
	 * A getter of Y coordinate.
	 * @return (Integer) X coordinate.
	 */
	public int getY_dir() { return y_dir; }

	/**
	 * A getter of animal size on the panel.
	 * @return (Integer) animal size.
	 */
	public int getSize() { return this.size; }

	/**
	 * A getter of animal's panel.
	 * @return (ZooPanel) panel to be used.
	 */
	public ZooPanel getPan() { return this.pan; }

	/**
	 * A ctor of animal name and location.
	 * Using logCtor function to print doc message.
	 *
	 * @param name
	 * 			name string of the animal.
	 * @param location
	 * 			location point of the animal.
	 */
	public Animal(String name, Point location)	{
		location = new Point(location);
		this.name = name;
		MessageUtility.logConstractor(getClass().getSimpleName(), getName());
	}

	/**
	 * A ctor of animal name only.
	 * Using logCtor function to print doc message.
	 *
	 * @param name
	 * 			name string of the animal.
	 */
	public Animal(String name) {
		super();
		this.name = name;
		MessageUtility.logConstractor(getClass().getSimpleName(), getName());
	}

	/**
	 * Abstract function required for Roar and Chew abstract classes.
	 */
	public abstract void makeSound();

	/**
	 * A simple function used to perform eating action.
	 *
	 * @param food
	 * 			IEdible food-type.
	 * @return true or false.
	 */
	public boolean eat(IEdible food) {
		DecimalFormat df = new DecimalFormat("#.##");
		double wgt = diet.eat(this, food);
		if (wgt > 0)
		{
			setWeight(getWeight()+Double.parseDouble(df.format(wgt)));
			return true;
		}
		return false;
	}

	/**
	 * Setter of animal diet class.
	 *
	 * @param diet
	 * 			diet class.
	 * @return true.
	 */
	public boolean setDiet(IDiet diet)
	{
		this.diet = diet;
		MessageUtility.logSetter(getClass().getSimpleName(), "setDiet", diet.getClass().getSimpleName(), true);
		return true;
	}

	/**
	 * Getter of animal diet class.
	 *
	 * @return object of diet class.
	 */
	public IDiet getDiet() {return this.diet; }

	/**
	 * Setter of animal weight.
	 * Using logSetter function to print doc message.
	 *
	 * @param weight
	 * 			(double) animal weight.
	 * @return true if weight is positive, otherwise false.
	 */
	public boolean setWeight(double weight)
	{
		if (weight > 0)
		{
			MessageUtility.logSetter(this.getClass().getSimpleName(), "setWeight", weight, true);
			this.weight = weight;
			return true;
		}
		MessageUtility.logSetter(this.getClass().getSimpleName(), "setWeight", weight, false);
		return false;
	}

	/**
	 * Getter of animal weight.
	 *
	 * @return animal weight.
	 */
	public double getWeight() {
		return this.weight; }

	/**
	 * A simple getter of animal food type.
	 * Using logGetter function to print doc message.
	 * @return EFoodType of this animal.
	 */
	public EFoodType getFoodType()
	{
		if (this instanceof Lion) {
			MessageUtility.logGetter(getName(), "getFoodType", EFoodType.NOTFOOD);
			return EFoodType.NOTFOOD; }
		MessageUtility.logGetter(getName(), "getFoodType", EFoodType.MEAT);
		return EFoodType.MEAT;
	}

	/**
	 * Getter of animal name.
	 * We use this getter inside the class.
	 * @return animal name.
	 */
	public String getName() { return this.name; }

	/**
	 * toString function of animal class.
	 * Using template: [!] animalName: total distance: [distance], weight: [weight]
	 * @return string of the object values.
	 */
	public String toString() { return "[!] " + this.name + ": total distance: " + location.getTotalDistance() + ", weight: " + getWeight(); }

	/**
	 * A simple getter of EAT_DISTANCE.
	 * @return (Integer) EAT_DISTANCE.
	 */
	public int getEAT_DISTANCE() {
		return EAT_DISTANCE;
	}

	/**
	 * A simple setter of location.
	 * @param other (Point) location to be applied.
	 * @return ture.
	 */
	public boolean setLocation(Point other)
	{
		this.location = other;
		return true;
	}

	/**
	 * A simple getter of location.
	 * @return (Point) location.
	 */
	public Point getLocation()
	{
		return this.location;
	}

	/**
	 * A simple setter of eatCount.
	 * @param eatCount (Integer) eat counter.
	 */
	public void setEatCount(int eatCount) {
		this.eatCount = eatCount;
	}
}
