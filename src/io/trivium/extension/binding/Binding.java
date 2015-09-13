/*
 * Copyright 2015 Jens Walter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.trivium.extension.binding;

import io.trivium.anystore.AnyClient;
import io.trivium.anystore.ObjectRef;
import io.trivium.extension._f70b024ca63f4b6b80427238bfff101f.TriviumObject;
import io.trivium.extension.type.Type;
import io.trivium.extension.type.TypeFactory;
import io.trivium.extension.type.Typed;
import io.trivium.Registry;

import java.util.logging.Logger;

public abstract class Binding implements Typed {
    protected Logger log = Logger.getLogger(getClass().getName());
    private State state = State.stopped;

    public abstract void start();

    public abstract void stop();

    public void check() throws Exception{}

    public State getState(){
        return state;
    }

    protected void setState(State newState){
        state=newState;
    }

    public String getName(){
        return this.getClass().getCanonicalName();
    }

    protected void emit(Type object){
        ObjectRef typeId = object.getTypeId();
        TypeFactory<Type> factory =  Registry.INSTANCE.typeFactory.get(typeId);
        TriviumObject obj = factory.getTriviumObject(object);
        AnyClient.INSTANCE.storeObject(obj);
    }
}
