package binder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class IdConvertion {
    HashMap<String, Integer> IdUser;
    HashMap<String, Integer> IdGame;

    public IdConvertion(){
        IdUser = new HashMap<>();
        IdGame = new HashMap<>();
    }

    public void fillTables(HashMap<String, HashMap<String, ArrayList<Integer>>> data){
        int idu = 0;
        int idg = 0;
        for(String user : data.keySet()){
            IdUser.put(user, idu);
            idu++;

            HashMap<String, ArrayList<Integer>> games = data.get(user);
            for(String game : games.keySet()){
                if(!IdGame.containsKey(game)){
                    IdGame.put(game, idg);
                    idg++;
                }
            }
        }
    }

    public String getOriginalUserId(int id){
        for(String user : IdUser.keySet()){
            if(IdUser.get(user) == id){
                return user;
            }
        }

        return null;
    }

    public int getNewUserId(String id){
        return IdUser.get(id);
    }

    public String getOriginalGameId(int id){
        for(String game : IdGame.keySet()){
            if(IdGame.get(game) == id){
                return game;
            }
        }

        return null;
    }

    public Set<String> getAllOriginalGameId(){
        return IdGame.keySet();
    }

    public int getNewGameId(String id){
        return IdGame.get(id);
    }
}
