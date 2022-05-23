package com.joinhocus.horus.misc.strgen;

import java.util.concurrent.ThreadLocalRandom;

public class WordStringGenerator implements RandomStringGenerator {

    private final String[][] WORDS = new String[][]{
            // animal names
            {
                    "dog",
                    "puppy",
                    "turtle",
                    "rabbit",
                    "parrot",
                    "cat",
                    "kitten",
                    "goldfish",
                    "mouse",
                    "hamster",
                    "cow",
                    "rabbit",
                    "ducks",
                    "shrimp",
                    "pig",
                    "goat",
                    "crab",
                    "deer",
                    "bee",
                    "sheep",
                    "fish",
                    "turkey",
                    "dove",
                    "chicken",
                    "horse",
                    "crow",
                    "peacock",
                    "dove",
                    "sparrow",
                    "goose",
                    "stork",
                    "pigeon",
                    "turkey",
                    "hawk",
                    "raven",
                    "parrot",
                    "flamingo",
                    "seagull",
                    "ostrich",
                    "swallow",
                    "penguin",
                    "robin",
                    "swan",
                    "owl",
                    "woodpecker",
                    "squirrel",
                    "dog",
                    "chimpanzee",
                    "ox",
                    "lion",
                    "panda",
                    "walrus",
                    "otter",
                    "mouse",
                    "kangaroo",
                    "goat",
                    "horse",
                    "monkey",
                    "cow",
                    "koala",
                    "mole",
                    "elephant",
                    "leopard",
                    "hippopotamus",
                    "giraffe",
                    "fox",
                    "coyote",
                    "hedgehong",
                    "sheep",
                    "deer"
            },
            // egyptian god names
            {
                    "amun",
                    "anubis",
                    "apep",
                    "apis",
                    "aten",
                    "atum",
                    "bast",
                    "bat",
                    "bes",
                    "horus",
                    "gep",
                    "hapy",
                    "hathor",
                    "heget",
                    "isis",
                    "iusaaset",
                    "khepry",
                    "khnum",
                    "khonsu",
                    "kuk",
                    "maahes",
                    "maat",
                    "mafdet",
                    "menhit",
                    "meretseger",
                    "meskhenet",
                    "menthu",
                    "min",
                    "mnevis",
                    "mut",
                    "naunet",
                    "neith",
                    "nekhbet",
                    "nephthys",
                    "nut",
                    "osiris",
                    "pakhet",
                    "ptah",
                    "qebui",
                    "ra",
                    "reshep",
                    "satis",
                    "sekhmet",
                    "seker",
                    "selket",
                    "sobek",
                    "set",
                    "seshat",
                    "shu",
                    "swenet",
                    "tatenen",
                    "tefnut",
                    "thoth",
                    "wadjet",
                    "wepwawet",
                    "wosretz"
            },
            // wizard names
            {
                    "iqium",
                    "endobahn",
                    "olagron",
                    "oqihr",
                    "stanitor",
                    "aluxium",
                    "adeveus",
                    "kregorim",
                    "olenor",
                    "ilimorn",
                    "osorin",
                    "drutaz",
                    "anvolenor",
                    "vaxar",
                    "groxius",
                    "utrix",
                    "alineus",
                    "izoleus",
                    "ozahl",
                    "khurnas",
                    "drineus",
                    "ulzaronin",
                    "ujahr",
                    "aphior",
                    "vozahr",
                    "urasorin",
                    "erhan",
                    "idalf",
                    "ukron",
                    "esior",
                    "merlin",
                    "gandalf",
                    "horris",
                    "uveus",
                    "idor",
                    "crethar",
                    "umazz",
                    "obahn",
                    "ajamar",
                    "dridius",
                    "urius",
                    "abeus",
                    "ilveqirax",
                    "albus"
            }
    };

    @Override
    public String generate(int length) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randIdx = ThreadLocalRandom.current().nextInt(WORDS.length);
            String[] possibilities = WORDS[randIdx];
            String chosen = possibilities[ThreadLocalRandom.current().nextInt(possibilities.length)];
            out.append(chosen).append("-");
        }

        String rand = out.toString();
        return rand.substring(0, rand.lastIndexOf('-'));
    }

    public static void main(String[] args) {
        int amount = 10;
        WordStringGenerator generator = new WordStringGenerator();
        for (int i = 0; i < amount; i++) {
            System.out.println(generator.generate(4));
        }
    }
}
