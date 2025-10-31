package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.InteractionType;
import com.focalizze.Focalizze.models.User;

public interface InteractionLimitService {

    public void checkInteractionLimit(User user);

    public void recordInteraction(User user, InteractionType type);

    public int getRemainingInteractions(User user);


}
