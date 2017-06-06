package net.stawrul.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import net.stawrul.model.Film;
import net.stawrul.model.Order;
import net.stawrul.services.exceptions.OutOfStockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Komponent (serwis) biznesowy do realizacji operacji na zamówieniach.
 */
@Service
public class OrdersService extends EntityService<Order> {

    //Instancja klasy EntityManger zostanie dostarczona przez framework Spring
    //(wstrzykiwanie zależności przez konstruktor).
    public OrdersService(EntityManager em) {

        //Order.class - klasa encyjna, na której będą wykonywane operacje
        //Order::getId - metoda klasy encyjnej do pobierania klucza głównego
        super(em, Order.class, Order::getId);
    }

    /**
     * Pobranie wszystkich zamówień z bazy danych.
     *
     * @return lista zamówień
     */
    public List<Order> findAll() {
        return em.createQuery("SELECT o FROM Order o", Order.class).getResultList();
    }

    /**
     * Złożenie zamówienia w sklepie.
     * <p>
     * Zamówienie jest akceptowane, jeśli wszystkie objęte nim produkty są dostępne (przynajmniej 1 sztuka). W wyniku
     * złożenia zamówienia liczba dostępnych sztuk produktów jest zmniejszana o jeden. Metoda działa w sposób
     * transakcyjny - zamówienie jest albo akceptowane w całości albo odrzucane w całości. W razie braku produktu
     * wyrzucany jest wyjątek OutOfStockException.
     *
     * @param order zamówienie do przetworzenia
     */
    @Transactional
    public void placeOrder(Order order) {
        //puste zamowienie
        if (order.getFilms().isEmpty()) throw new OutOfStockException();

        //duplikaty
        Set<Film> set = new HashSet<Film>(order.getFilms());
        if(set.size() < order.getFilms().size()){
            throw new OutOfStockException();
        }
        
        //nie wiecej niz x produktow w zamowieniu
        int x=3;
        if (order.getFilms().size()>x) throw new OutOfStockException();
        
        
        int priceX = 10;
        int price = 0;
        
        //zamowienia parami
        Map<UUID, Integer> pary = new HashMap<UUID, Integer>();
        
        for (Film filmStub : order.getFilms()) {
            Film film = em.find(Film.class, filmStub.getId());
            price+=film.getPrice();
            pary.putIfAbsent(film.getId(), 0);
            pary.put(film.getId(), pary.get(film.getId())+1);
            if (film.getAmount() < 1) {
                //wyjątek z hierarchii RuntineException powoduje wycofanie transakcji (rollback)
                throw new OutOfStockException();
            } else {
                int newAmount = film.getAmount() - 1;
                film.setAmount(newAmount);
            }
        }
        
        for (Map.Entry<UUID, Integer> para : pary.entrySet())
        {
           if (para.getValue()%2==1) throw new OutOfStockException();
        }
        
        if (price<priceX) throw new OutOfStockException();

        //jeśli wcześniej nie został wyrzucony wyjątek OutOfStockException, zamówienie jest zapisywane w bazie danych
        save(order);
    }
}
