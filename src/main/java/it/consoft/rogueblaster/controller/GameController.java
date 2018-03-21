package it.consoft.rogueblaster.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.qos.logback.core.net.SyslogOutputStream;
import it.consoft.rogueblaster.model.ChestModel;
import it.consoft.rogueblaster.model.EnemyModel;
import it.consoft.rogueblaster.model.MainCharModel;
import it.consoft.rogueblaster.model.MapModel;
import it.consoft.rogueblaster.model.enumeration.AttrEnum;
import it.consoft.rogueblaster.model.enumeration.MapSizeEnum;

@Controller
@EnableScheduling
@SuppressWarnings("unused")
public class GameController {

	private MainCharModel mc = null;
	private MapModel map = new MapModel();

	private int[] charCoords;
	private List<int[]> enemiesCoords;

	@RequestMapping(value = "/start")
	@ResponseBody
	public String startGameGet(@RequestParam(name = "sz", required = false, defaultValue = "-1") String sz) {
		this.mc = new MainCharModel();
		int size = Integer.parseInt(sz);
		if (size > 0) {
			switch (size) {
			case 1: {
				this.map = new MapModel(MapSizeEnum.SMALL);
				break;
			}
			case 2: {
				this.map = new MapModel(MapSizeEnum.MEDIUM);
				break;
			}
			case 3: {
				this.map = new MapModel(MapSizeEnum.BIG);
				break;
			}
			default: {
				break;
			}
			}
			this.setup();
			return this.map.toJSON();
		}
		return this.mc.toJSON();
	}

	@PostMapping(value = "/start")
	public String startGamePost(@ModelAttribute MapModel start) {
		this.map = new MapModel(MapSizeEnum.SMALL);
		return start.toString();
	}

	@GetMapping(value = "/getmap")
	public String getMapStatus() {
		return "";
	}

	public void gameOver() {

	}

	public void update() {

	}

	private void setup() {
		EnemyModel enemy;
		ChestModel chest;
		this.enemiesCoords = new ArrayList<int[]>();

		int[] coords = this.generateCoords();
		while (!this.map.setTileContent(coords[0], coords[1], this.mc)) {
			System.out.println("Inserimento mainCharacter non riuscito alle coordinate: " + coords[0] + "," + coords[1]);
		}
		this.charCoords = coords;
		this.map.setMainAlive(true);
		System.out.println("Inserimento mainCharacter riuscito alle coordinate: " + coords[0] + "," + coords[1]);
		// blocco nemico
		for (int i = 0; i < this.map.getMaxEnemy(); i++) {
			enemy = new EnemyModel();
			coords = this.generateCoords();
			while (!this.map.setTileContent(coords[0], coords[1], enemy)) {
				System.out.println(
						"Inserimento Enemy " + i + " non riuscito alle coordinate: " + coords[0] + "," + coords[1]);
				coords = this.generateCoords();
			}
			this.enemiesCoords.add(coords);
			System.out.println("Inserimento Enemy " + i + " riuscito alle coordinate: " + coords[0] + "," + coords[1]);
		}
		
		// blocco tesoro
		for (int i = 0; i < this.map.getMaxTeasure(); i++) {
			chest = new ChestModel((AttrEnum.getById((int) (Math.random() * 4) + 1)));
			coords = this.generateCoords();
			while (!this.map.setTileContent(coords[0], coords[1], chest)) {
				System.out.println(
						"Inserimento Teasure " + i + " non riuscito alle coordinate: " + coords[0] + "," + coords[1]);
				coords = this.generateCoords();
			}
			System.out
					.println("Inserimento Teasure " + i + " riuscito alle coordinate: " + coords[0] + "," + coords[1]);
		}
		System.out.println(this.map);
	}

	private int[] generateCoords() {
		int[] result = new int[2];
		result[0] = (int) (Math.random() * this.map.getWidth());
		result[1] = (int) (Math.random() * this.map.getHeight());
		return result;
	}

	@Scheduled(fixedRate = 2000, initialDelay = 10000)
	private void simulate() {
		try {
			int newX = this.charCoords[0] + ((int) (Math.random() * 3)) - 1;
			int newY = this.charCoords[1] + ((int) (Math.random() * 3)) - 1;
			while (!this.map.moveMainChar(this.charCoords[0], this.charCoords[1], newX, newY)) {
				newX = this.charCoords[0] + ((int) (Math.random() * 3)) - 1;
				newY = this.charCoords[1] + ((int) (Math.random() * 3)) - 1;
			}
			this.charCoords[0] = newX;
			this.charCoords[1] = newY;
			for (int i = 0; i < this.enemiesCoords.size(); i++) {
				newX = this.enemiesCoords.get(i)[0] + ((int) (Math.random() * 3)) - 1;
				newY = this.enemiesCoords.get(i)[1] + ((int) (Math.random() * 3)) - 1;
				while (this.map.moveEnemy(this.enemiesCoords.get(i)[0], this.enemiesCoords.get(i)[1], newX, newY) == -1 ) {
					newX = this.enemiesCoords.get(i)[0] + ((int) (Math.random() * 3)) - 1;
					newY = this.enemiesCoords.get(i)[1] + ((int) (Math.random() * 3)) - 1;
				}
				if (!this.map.isMainAlive()) {
					System.exit(0);
				}
				this.enemiesCoords.get(i)[0] = newX;
				this.enemiesCoords.get(i)[1] = newY;
			}
			System.out.println(this.mc.toJSON());
			System.out.println("Main: " + this.charCoords[0] + " " + this.charCoords[1]);
			System.out.println("Enemy one: " + this.enemiesCoords.get(1)[0] + " " + this.enemiesCoords.get(1)[1]);
			System.out.println("Enemy two: " + this.enemiesCoords.get(0)[0] + " " + this.enemiesCoords.get(0)[1]);
			System.out.println("Main Life: " + this.mc.getVit());
			System.out.println(this.map);
		} catch (IndexOutOfBoundsException e) {
			return;
		}
	}

}
