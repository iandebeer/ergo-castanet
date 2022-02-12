--let List/map = ./Prelude/List/map.dhall
--let List/concat = ./Prelude/List/concat.dhall

let List/map = ./modules/client/src/main/resources/Prelude/List/map.dhall
let List/concat = ./modules/client/src/main/resources/Prelude/List/concat.dhall


let RPC : Type = {name : Text, input : Text,  output : Text}
let Service : Type = {packageName : Text, name : Text, rpcs: List RPC} 
let ProtoItem = <R : RPC | S : Service>

let Place: Type = {id : Natural, name : Text, capacity : Natural}
let Transition : Type = {id : Natural, name : Text , service : Service, rpc : RPC}
let LinkableElement = < P: Place | T : Transition >

let Timed : Type = {from: Natural, to: Natural, interval: Natural}
let Weighted : Type = {from: Natural, to: Natural, weight: Natural}
let Arc = <T: Timed | W: Weighted>

let PetriElement = < A : Arc | L : LinkableElement >
let PetriElements : Type = List LinkableElement

let nextNumber = \(i : Natural) -> i + 1
let linkablePlace = List/map Place LinkableElement (\(p : Place) -> LinkableElement.P p)
let linkableTransition = List/map Transition LinkableElement (\(t : Transition) -> LinkableElement.T t)
let petriElements = List/concat LinkableElement [linkablePlace ./modules/client/src/main/resources/places.dhall, linkableTransition ./modules/client/src/main/resources/transitions.dhall]
-- let petriElements = List/concat LinkableElement [linkablePlace ./places.dhall, linkableTransition ./transitions.dhall]

in  petriElements