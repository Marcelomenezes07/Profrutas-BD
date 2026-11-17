package model;

/** Item simples (codigo + nome) usado para preencher listas de selecao (combos). */
public record ItemLista(int id, String nome) {
    @Override
    public String toString() {
        return id == 0 ? nome : id + " - " + nome;
    }
}
