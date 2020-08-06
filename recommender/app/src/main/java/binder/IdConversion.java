/*

    Copyright 2020 Guillaume Longrais

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package binder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class IdConversion {
    HashMap<String, Integer> IdUser;
    HashMap<String, Integer> IdGame;

    public IdConversion(){
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

    public Set<String> getAllOriginalUserId(){
        return IdUser.keySet();
    }

    public int getNewGameId(String id){
        return IdGame.get(id);
    }
}
