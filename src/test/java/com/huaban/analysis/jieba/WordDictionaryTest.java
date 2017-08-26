package com.huaban.analysis.jieba;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WordDictionaryTest extends TestCase {

    private static final String TERM="豐碩";
    private static final String FILE_PATH="conf/dict_big.txt";
    private File file;

    @Before
    @Override
    protected void setUp() throws Exception {
        file = new File(FILE_PATH);
        //WordDictionary.getInstance().init(new File("conf"));
    }

    private boolean test(){
        WordDictionary.getInstance().loadUserDict(file);
        boolean isContainsWord = WordDictionary.getInstance().containsWord(TERM);
        assertTrue(isContainsWord);
        return isContainsWord;
    }
    @Test
    public void test01_singleThread(){
        WordDictionary.getInstance().loadUserDict(file);
        boolean isContainsWord = WordDictionary.getInstance().containsWord(TERM);
        assertTrue(isContainsWord);
    }

    @Test
    public void test02_multiThread(){
        int threadSize= 10;
        ExecutorService service = Executors.newCachedThreadPool();
        List<Future<?>> futures = new ArrayList<Future<?>>();
        for(int i=0;i<threadSize;i++){
            Future<?> future =service.submit(new Callable<Boolean>(){
                @Override
                public Boolean call() throws Exception {
                    WordDictionary.getInstance().loadUserDict(file);
                    boolean isContainsWord = WordDictionary.getInstance().containsWord(TERM);
                    assertTrue(isContainsWord);
                    return isContainsWord;
                }
            });
            futures.add(future);
        }

        for(Future<?> future: futures){
            try{
                boolean isContains = (Boolean)future.get();
                Assert.assertTrue(isContains);
            }catch (Exception e){
                Assert.assertNotNull(e);
            }
        }
    }
}
