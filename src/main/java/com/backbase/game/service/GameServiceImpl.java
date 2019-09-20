package com.backbase.game.service;

import com.backbase.game.repository.GameRepository;
import com.backbase.game.repository.dao.GameDAO;
import com.backbase.game.service.bo.BoardConfig;
import com.backbase.game.service.bo.Game;
import com.backbase.game.service.bo.Player;
import com.backbase.game.service.mappers.GameMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
class GameServiceImpl implements GameService {

    private GameRepository gameRepository;
    private GameMapper gameMapper;

    public GameServiceImpl(GameRepository gameRepository, GameMapper gameMapper) {
        this.gameRepository = gameRepository;
        this.gameMapper = gameMapper;
    }

    @Override
    public Game createGame(String uri) {

        Map<Integer, Integer> initialBoard = BoardConfig.INITIAL_BOARD;

        Game newGame = new Game();

        newGame.setUri(uri);
        newGame.setBoard(initialBoard);
        newGame.setCurrentPlayer(Player.FIRST_PLAYER.getPlayer());

        Game gameCreated = gameMapper.getGame(gameRepository.save(gameMapper.getGameDAO(newGame)));

        gameCreated.setUri(String.format("%s/%s", uri, gameCreated.getId()));

        return gameMapper.getGame(gameRepository.save(gameMapper.getGameDAO(gameCreated)));
    }

    @Override
    public Game makeMove(int gameId, int pitId) {

        Optional<GameDAO> gameDAO = gameRepository.findById(gameId);

        if (gameDAO.isPresent()) {
            Game game = gameMapper.getGame(gameDAO.get());

            if (isValidMove(game, pitId)){
                moveStones(game, pitId);
                gameRepository.save(gameMapper.getGameDAO(game));

                return game;
            }

            // throws move not valid exception
            return new Game();
        }

        // throws game not found exception
        return new Game();
    }

    void moveStones(Game game, int pitId) {
        Map<Integer, Integer> board = game.getBoard();
        Player player = Player.valueOf(game.getCurrentPlayer());
        int pitStones = board.get(pitId);
        board.put(pitId, 0);

        //Need to make validation of opponent kalah.
        for(int i = pitId + 1; i <= pitId + pitStones; i++) {

            int pitToFill = i;

            if(i > BoardConfig.SECOND_PLAYER_KALAH) {
                pitToFill = i - BoardConfig.SECOND_PLAYER_KALAH;
            }

            board.put(pitToFill, board.get(pitToFill) + 1 );
            //Last stone
            if (pitToFill == pitId + pitStones) {
                int lastPitStone = pitId + pitStones;

                // If last pit stone is empty takes opponent stones and loose turn
                if (board.get(lastPitStone) == 0){
                    int opponentPit = BoardConfig.SECOND_PLAYER_KALAH - lastPitStone;
                    int opponentStones = board.get(opponentPit);
                    board.put(opponentPit, 0);
                    board.put(player.getKalahId(), opponentStones + 1);
                    game.setCurrentPlayer(player.getOppositePlayer().toString());
                }

                if(player.getKalahId() == lastPitStone) {
                    //Player Keep playing
                }
                else {
                    game.setCurrentPlayer(player.getOppositePlayer().toString());
                }
            }
        }
    }

    boolean isValidMove(Game game, int starterPit) {
        boolean validMove = false;
        Player currentPlayer = Player.valueOf(game.getCurrentPlayer());

        switch (currentPlayer){
            case FIRST_PLAYER:
                validMove = starterPit < BoardConfig.FIRST_PLAYER_KALAH;
                break;
            case SECOND_PLAYER:
                validMove = starterPit < BoardConfig.SECOND_PLAYER_KALAH && starterPit > BoardConfig.FIRST_PLAYER_KALAH;
                break;
            default:
                break;
        }

        return validMove;
    }
}
