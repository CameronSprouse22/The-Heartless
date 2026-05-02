package com.heartless.model;

import com.heartless.model.enums.PlayerStatusEnum;
import com.heartless.operation.item.ItemsInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A participant in a game — both VIP (host) and invited players.
 */
public class Player {

    private final String id;
    private String name;
    private String email;
    private String phone;
    private PlayerStatusEnum status;
    private boolean isDead;
    private boolean isTraitor;
    private boolean hasBeenRevealed;
    private List<ItemsInterface> items;
    private Card card;

    public Player(String name, String email, String phone) {
        validateName(name);
        validateContact(email, phone);
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = PlayerStatusEnum.PENDING;
        this.isDead = false;
        this.isTraitor = false;
        this.hasBeenRevealed = false;
        this.items = new ArrayList<>();
        this.card = null;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or blank");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("Name must be 50 characters or fewer");
        }
    }

    private void validateContact(String email, String phone) {
        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            throw new IllegalArgumentException("At least one of email or phone must be provided");
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { validateName(name); this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public PlayerStatusEnum getStatus() { return status; }
    public void setStatus(PlayerStatusEnum status) { this.status = Objects.requireNonNull(status); }
    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { isDead = dead; }
    public boolean isTraitor() { return isTraitor; }
    public void setTraitor(boolean traitor) { isTraitor = traitor; }
    public boolean isHasBeenRevealed() { return hasBeenRevealed; }
    public void setHasBeenRevealed(boolean hasBeenRevealed) { this.hasBeenRevealed = hasBeenRevealed; }
    public List<ItemsInterface> getItems() { return items; }
    public void setItems(List<ItemsInterface> items) { this.items = Objects.requireNonNull(items); }
    public Card getCard() { return card; }
    public void setCard(Card card) { this.card = card; }

    /**
     * Returns the contact info used for invitation (email preferred over phone).
     */
    public String getContact() {
        return (email != null && !email.isBlank()) ? email : phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Player{id='" + id + "', name='" + name + "', status=" + status + "}";
    }
}
