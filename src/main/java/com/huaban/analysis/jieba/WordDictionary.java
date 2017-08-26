package com.huaban.analysis.jieba;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class WordDictionary {
    private static WordDictionary singleInstance = new WordDictionary();
    private static final String MAIN_DICT = "/dict.txt";
    private static String USER_DICT_SUFFIX = ".dict";

    static {
        singleInstance.loadDict();
    }

    public final TrieNode trie = new TrieNode();
    public final Map<String, Word> freqs = new HashMap<String, Word>();
    private Calendar calendar = Calendar.getInstance();
    private Double minFreq = Double.MAX_VALUE;
    private Double total = 0.0;
    private static boolean isLoaded = false;
    private static long lastModifiedTimestamp=0l;


    private WordDictionary() {
    }


    public static WordDictionary getInstance() {
        return singleInstance;
    }


    /**
     * for ES to initialize the user dictionary.
     *
     * @param configFile
     */
    public synchronized void init(File configFile) {
        if (!isLoaded) {
            for (File userDict : configFile.listFiles()) {
                if (userDict.getPath().endsWith(USER_DICT_SUFFIX)) {
                    singleInstance.loadUserDict(userDict);
                }
            }
            isLoaded = true;
        }
    }


    public void loadDict() {
        InputStream is = this.getClass().getResourceAsStream(MAIN_DICT);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

            long s = System.currentTimeMillis();
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");

                if (tokens.length < 3)
                    continue;

                String word = tokens[0];
                String tokenType = tokens[2];
                double freq = Double.valueOf(tokens[1]);
                total += freq;
                word = addWord(word);
                freqs.put(word, Word.createWord(word, freq, tokenType));
            }
            // normalize
            for (Entry<String, Word> entry : freqs.entrySet()) {
                entry.getValue().setFreq(Math.log(entry.getValue().getFreq() / total));
                minFreq = Math.min(entry.getValue().getFreq(), minFreq);
            }
            System.out.println(String.format("main dict load finished, time elapsed %d ms",
                    System.currentTimeMillis() - s));
        }
        catch (IOException e) {
            System.err.println(String.format("%s load failure!", MAIN_DICT));
        }
        finally {
            try {
                if (null != is)
                    is.close();
            }
            catch (IOException e) {
                System.err.println(String.format("%s close failure!", MAIN_DICT));
            }
        }
    }


    private synchronized String addWord(String word) {
        TrieNode p = this.trie;
        StringBuilder r = new StringBuilder();
        for (char ch : word.toCharArray()) {
            ch = CharacterUtil.regularize(ch);
            r.append(ch);
            if (ch == ' ')
                continue;
            TrieNode pChild = null;
            if ((pChild = p.childs.get(ch)) == null) {
                pChild = new TrieNode();
                p.childs.put(ch, pChild);
            }
            p = pChild;
        }
        p.childs.put(' ', null);
        return r.toString();
    }


    public synchronized void loadUserDict(File userDict) {
        if(!userDict.exists()){
            System.err.println(String.format("could not find %s", userDict.getAbsolutePath()));
            return;
        }

        //Watch userDict if changed or not
        long start = System.currentTimeMillis();
        if(lastModifiedTimestamp == 0l){
            lastModifiedTimestamp = userDict.lastModified();
        }else if(lastModifiedTimestamp!=userDict.lastModified()){
            lastModifiedTimestamp = userDict.lastModified();
        }else{

            calendar.setTimeInMillis(lastModifiedTimestamp);
            String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(calendar.getTime());
            System.out.println(String.format("[%s] userDict not changed since:%s"
                    , Thread.currentThread().getName()
                    , timeStamp)));
            System.out.println(String.format("[%s] user dict %s load finished, tot words:%d, time elapsed:%dms"
                    , Thread.currentThread().getName()
                    , userDict.getAbsolutePath()
                    , freqs.size()
                    , System.currentTimeMillis() - start));
            return;
        }

        InputStream is = null;
        try {
            is = new FileInputStream(userDict);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            int count = 0;
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");

                if (tokens.length < 3)
                    continue;

                String word = tokens[0];
                String tokenType = tokens[2];
                double freq = Double.valueOf(tokens[1]);
                word = addWord(word);
                freqs.put(word, Word.createWord(word, Math.log(freq / total), tokenType));
                count++;
            }
            System.out.println(String.format("[%s]user dict %s load finished, tot words:%d, time elapsed:%dms"
                    , Thread.currentThread().getName()
                    , userDict.getAbsolutePath()
                    , count
                    , System.currentTimeMillis() - start));
        }
        catch (IOException e) {
            System.err.println(String.format("%s: load user dict failure!", userDict.getAbsolutePath()));
        }
        finally {
            try {
                if (null != is)
                    is.close();
            }
            catch (IOException e) {
                System.err.println(String.format("%s close failure!", userDict.getAbsolutePath()));
            }
        }
    }


    public TrieNode getTrie() {
        return this.trie;
    }


    public boolean containsWord(String word) {
        return freqs.containsKey(word);
    }


    public Word getWord(String token) {
        if (containsWord(token)) {
            return freqs.get(token);
        }
        else {
            return null;
        }
    }


    public Double getFreq(String key) {
        if (containsWord(key))
            return freqs.get(key).getFreq();
        else
            return minFreq;
    }
}
